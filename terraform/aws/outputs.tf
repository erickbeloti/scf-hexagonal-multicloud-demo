output "function_url" {
  description = "Lambda Function URL endpoint for AWS Lambda functions"
  value       = aws_lambda_function_url.function_url.function_url
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.task_functions.function_name
}

output "dynamodb_table_name" {
  description = "DynamoDB table name"
  value       = aws_dynamodb_table.tasks.name
}

output "test_commands" {
  description = "Example test commands"
  value = {
    create_task = "curl -X POST '${aws_lambda_function_url.function_url.function_url}' -H 'Content-Type: application/json' -H 'function.name: createTask' -d '{\"userId\":\"user123\",\"description\":\"AWS Lambda task\",\"priority\":\"HIGH\"}'"
    list_tasks  = "curl -X POST '${aws_lambda_function_url.function_url.function_url}' -H 'Content-Type: application/json' -H 'function.name: listTasksByUser' -d '{\"userId\":\"user123\",\"page\":0,\"size\":10}'"
    get_task    = "curl -X POST '${aws_lambda_function_url.function_url.function_url}' -H 'Content-Type: application/json' -H 'function.name: getTaskById' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\"}'"
    update_task = "curl -X POST '${aws_lambda_function_url.function_url.function_url}' -H 'Content-Type: application/json' -H 'function.name: updateTask' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\",\"description\":\"Updated task\",\"priority\":\"MEDIUM\"}'"
    delete_task = "curl -X POST '${aws_lambda_function_url.function_url.function_url}' -H 'Content-Type: application/json' -H 'function.name: deleteTask' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\"}'"
  }
}
