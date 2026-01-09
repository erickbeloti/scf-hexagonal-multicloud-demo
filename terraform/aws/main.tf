terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Check if JAR file exists and track changes
data "local_file" "jar_file" {
  filename = "${path.module}/../../target/deploy/tasks-0.1-multicloud-aws.jar"
}

# S3 bucket for Lambda code
resource "aws_s3_bucket" "lambda_bucket" {
  bucket = "${var.project_id}-lambda-code-${var.environment}"

  tags = {
    Name        = "Lambda Code Storage"
    Environment = var.environment
  }
}

# S3 bucket versioning
resource "aws_s3_bucket_versioning" "lambda_bucket_versioning" {
  bucket = aws_s3_bucket.lambda_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Upload Lambda JAR file directly to S3 without repackaging
resource "aws_s3_object" "lambda_code" {
  bucket = aws_s3_bucket.lambda_bucket.id
  key    = "tasks-0.1-multicloud-aws-${filemd5(data.local_file.jar_file.filename)}.jar"
  source = data.local_file.jar_file.filename
  etag   = filemd5(data.local_file.jar_file.filename)
}

# IAM role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "task-management-lambda-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Attach basic Lambda execution policy
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# DynamoDB permissions
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "lambda-dynamodb-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Resource = [
          aws_dynamodb_table.tasks.arn,
          "${aws_dynamodb_table.tasks.arn}/index/*",
          "arn:aws:dynamodb:${var.aws_region}:*:table/task_entity",
          "arn:aws:dynamodb:${var.aws_region}:*:table/task_entity/index/*"
        ]
      }
    ]
  })
}

# DynamoDB Table
resource "aws_dynamodb_table" "tasks" {
  name           = "tasks-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name     = "UserIdIndex"
    hash_key = "userId"
    range_key = "createdAt"
    projection_type = "ALL"
    read_capacity = 5
    write_capacity = 5
  }

  tags = {
    Name        = "Tasks Table"
    Environment = var.environment
  }
}

# Lambda Function - Using S3
resource "aws_lambda_function" "task_functions" {
  function_name    = "task-management-${var.environment}"
  role             = aws_iam_role.lambda_role.arn
  handler          = "org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest"
  runtime          = "java21"
  memory_size      = 512
  timeout          = 30

  # Using S3 with JAR file directly
  s3_bucket        = aws_s3_bucket.lambda_bucket.bucket
  s3_key           = aws_s3_object.lambda_code.key
  s3_object_version = aws_s3_object.lambda_code.version_id

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE = "aws"
      AWS_DYNAMODB_TABLE_NAME = aws_dynamodb_table.tasks.name
      MAIN_CLASS = "com.example.tasks.TaskApplication"
    }
  }

  tags = {
    Name        = "Task Management Functions"
    Environment = var.environment
  }
}

# Lambda Function URL
resource "aws_lambda_function_url" "function_url" {
  function_name      = aws_lambda_function.task_functions.function_name
  authorization_type = "NONE"

  cors {
    allow_credentials = true
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["content-type", "x-amz-date", "authorization", "x-api-key", "x-amz-security-token", "function.name"]
    expose_headers    = ["date", "keep-alive"]
    max_age           = 86400
  }
}

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "function_logs" {
  name              = "/aws/lambda/${aws_lambda_function.task_functions.function_name}"
  retention_in_days = 14
}
