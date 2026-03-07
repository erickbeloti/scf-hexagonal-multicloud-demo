# Building Portable Cloud Functions with Spring Cloud Function and Hexagonal Architecture — Part 2: AWS Lambda Implementation

In [Part 1](link-to-part-1), we built a Task Management System using Spring Cloud Function and Hexagonal Architecture, deploying it to Google Cloud Functions with Firestore. The power of this approach becomes evident in Part 2: **we'll deploy the exact same business logic to AWS Lambda with DynamoDB, changing only infrastructure adapters**.

No domain logic modifications. No business rule changes. Just infrastructure swapping.

This is the promise of **true cloud portability**.

## Recap: What We Built

Our Task Management System implements comprehensive business rules:

**Create Operations:**
- Users cannot create more than 5 high-priority tasks per day
- Task descriptions must be unique per user per day  
- Users cannot have more than 50 open tasks
- Tasks must have valid priorities (LOW, MEDIUM, HIGH)

**Read/Update/Delete Operations:**
- Users can only access their own tasks
- Completed tasks cannot be modified

These rules live in our **domain layer** — completely cloud-agnostic.

## The AWS Implementation Strategy

### What Stays the Same (Zero Changes)
✅ **Domain entities** — Task, TaskId, UserId, Priority, Status  
✅ **Business rules** — All validation logic remains identical  
✅ **Application services** — TaskService orchestration unchanged  
✅ **Use case interfaces** — Same inbound ports  
✅ **Function definitions** — Same Spring Cloud Function beans  

### What Changes (Infrastructure Only)
🔄 **Outbound adapter** — DynamoDB replaces Firestore  
🔄 **Maven configuration** — AWS dependencies replace GCP  
🔄 **Infrastructure as Code** — Terraform for AWS resources  
🔄 **Configuration** — AWS-specific application properties  

## AWS-Specific Implementation

### 1. Maven Dependencies

The project uses Spring Cloud AWS with a profile-based approach:

```xml
<profiles>
    <profile>
        <id>aws</id>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-function-adapter-aws</artifactId>
            </dependency>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-starter-dynamodb</artifactId>
            </dependency>
            <dependency>
                <groupId>com.amazonaws</groupId>
                <artifactId>aws-lambda-java-core</artifactId>
            </dependency>
            <dependency>
                <groupId>com.amazonaws</groupId>
                <artifactId>aws-lambda-java-events</artifactId>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

### 2. DynamoDB Entity Mapping

AWS DynamoDB requires a bean class with appropriate annotations:

```java
@DynamoDbBean
public class TaskEntity {
    private String id;
    private String userId;
    private String description;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    @DynamoDbSecondaryPartitionKey(indexNames = "UserIdIndex")
    public String getUserId() { return userId; }

    @DynamoDbSecondarySortKey(indexNames = "UserIdIndex")
    public String getCreatedAt() { return createdAt; }

    // Conversion methods between domain and persistence models
    public static TaskEntity fromDomain(Task task) {
        TaskEntity entity = new TaskEntity();
        entity.setId(task.getId().value());
        entity.setUserId(task.getUserId().value());
        entity.setDescription(task.getDescription());
        entity.setPriority(task.getPriority().name());
        entity.setStatus(task.getStatus().name());
        entity.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        entity.setUpdatedAt(task.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }

    public Task toDomain() {
        return Task.reconstitute(
            TaskId.of(this.id), UserId.of(this.userId), this.description,
            Priority.valueOf(this.priority), Status.valueOf(this.status),
            LocalDateTime.parse(this.createdAt), LocalDateTime.parse(this.updatedAt)
        );
    }
}
```

### 3. DynamoDB Repository Implementation

```java
@Repository
@Profile("aws")
public class DynamoDBTaskRepository implements TaskRepositoryPort {

    private final DynamoDbTable<TaskEntity> taskTable;

    public DynamoDBTaskRepository(DynamoDbTemplate dynamoDbTemplate, DynamoDbTable<TaskEntity> taskTable) {
        this.taskTable = taskTable;
    }

    @Override
    public Task save(Task task) {
        TaskEntity entity = TaskEntity.fromDomain(task);
        taskTable.putItem(entity);
        return task;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        Key key = Key.builder().partitionValue(id.value()).build();
        TaskEntity entity = taskTable.getItem(key);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return taskTable.scan().items().stream()
            .filter(entity -> userId.value().equals(entity.getUserId()))
            .filter(entity -> "HIGH".equals(entity.getPriority()))
            .filter(entity -> {
                LocalDateTime createdAt = LocalDateTime.parse(entity.getCreatedAt());
                return createdAt.isAfter(startOfDay) && createdAt.isBefore(endOfDay);
            })
            .count();
    }

    @Override
    public void deleteById(TaskId id) {
        Key key = Key.builder().partitionValue(id.value()).build();
        taskTable.deleteItem(key);
    }
}
```

**Note:** This implementation uses `scan()` with stream filtering for simplicity. In production with large datasets, use the `UserIdIndex` GSI with `query()` operations.

### 4. AWS Configuration

```java
@Configuration
@Profile("aws")
public class AwsConfiguration {

    @Value("${AWS_DYNAMODB_TABLE_NAME:tasks-dev}")
    private String tableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<TaskEntity> taskTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(tableName, TableSchema.fromBean(TaskEntity.class));
    }
}
```

## Infrastructure as Code: Terraform for AWS

### Main Resources

```hcl
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
    name            = "UserIdIndex"
    hash_key        = "userId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }
}

# Lambda Function
resource "aws_lambda_function" "task_functions" {
  function_name = "task-management-${var.environment}"
  role          = aws_iam_role.lambda_role.arn
  handler       = "org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest"
  runtime       = "java21"
  memory_size   = 512
  timeout       = 30

  s3_bucket = aws_s3_bucket.lambda_bucket.bucket
  s3_key    = aws_s3_object.lambda_code.key

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE  = "aws"
      AWS_DYNAMODB_TABLE_NAME = aws_dynamodb_table.tasks.name
      MAIN_CLASS              = "com.example.tasks.TaskApplication"
    }
  }
}

# Lambda Function URL (simpler than API Gateway)
resource "aws_lambda_function_url" "function_url" {
  function_name      = aws_lambda_function.task_functions.function_name
  authorization_type = "NONE"

  cors {
    allow_origins = ["*"]
    allow_methods = ["*"]
    allow_headers = ["content-type", "function.name"]
  }
}
```

## Deployment Process

### Build and Deploy
```bash
# Build AWS-specific JAR
mvn clean package -Paws

# Deploy infrastructure
cd terraform/aws
terraform init
terraform apply

# Get Lambda Function URL
terraform output function_url
```

## Testing in Action

### Function Routing
Spring Cloud Function uses a routing mechanism to direct requests:

```bash
# Create a task
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{
    "userId": "user123",
    "description": "AWS Lambda task",
    "priority": "HIGH"
  }'

# List user tasks
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: listTasksByUser" \
  -d '{"userId": "user123", "page": 0, "size": 10}'
```

### Business Rules Validation
```bash
# Try to create 6th high-priority task (should fail)
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{
    "userId": "user123",
    "description": "Sixth high priority task",
    "priority": "HIGH"
  }'

# Response: HTTP 422
{
  "status": 422,
  "errors": [{
    "code": "BUSINESS_RULE_VIOLATION",
    "message": "Cannot create more than 5 high priority tasks per day"
  }]
}
```

## Key Benefits Demonstrated

### 1. **Pure Business Logic**
The domain layer contains no AWS-specific code. Rules like "max 5 high-priority tasks per day" are expressed in pure Java.

### 2. **Testable Architecture**
```java
@Test
void shouldEnforceHighPriorityTaskLimit() {
    // Given: User already has 5 high-priority tasks today
    when(validator.countHighPriorityTasksToday("user123")).thenReturn(5);
    
    // When & Then: 6th task creation should fail
    assertThatThrownBy(() -> 
        Task.create("user123", "Task 6", HIGH, validator)
    ).isInstanceOf(TaskBusinessRuleException.class)
     .hasMessage("Cannot create more than 5 high priority tasks per day");
}
```

### 3. **Infrastructure Flexibility**
Swapping from Firestore to DynamoDB requires only:
- New outbound adapter implementation
- Configuration change
- Zero domain logic changes

## What's Coming Next

**Part 3: Azure Implementation**
- Same domain code
- Azure Functions adapter
- Cosmos DB repository
- Terraform for Azure resources

## The Power of True Portability

Our AWS implementation proves that **Hexagonal Architecture + Spring Cloud Function** delivers on its promise:

✅ **Zero business logic changes** — Rules work identically  
✅ **Minimal infrastructure code** — Only repository adapter needed  
✅ **Same development experience** — Testing and debugging remain consistent  
✅ **Risk mitigation** — True cloud portability reduces vendor lock-in  

## Get Started

Ready to deploy the same Task Management System to AWS Lambda?

🔗 **Complete source code:** [GitHub Repository](https://github.com/erickbeloti/scf-hexagonal-multicloud-demo)

### Repository Structure After AWS Implementation
```
├── src/main/java/com/example/tasks/
│   ├── domain/                    # ✅ Unchanged - same business rules
│   ├── application/               # ✅ Unchanged - same use cases  
│   └── adapters/
│       ├── inbound/functions/     # ✅ Unchanged - same Spring Cloud Functions
│       └── outbound/
│           ├── gcp/              # 🔵 GCP Firestore implementation
│           └── aws/              # 🟡 NEW: AWS DynamoDB implementation
├── terraform/
│   ├── gcp/                      # 🔵 GCP infrastructure
│   └── aws/                      # 🟡 NEW: AWS infrastructure
└── pom.xml                       # 🔄 Added AWS profile
```

### Quick Commands Reference
```bash
# Build for AWS
mvn clean package -Paws

# Deploy to AWS
cd terraform/aws && terraform apply

# Test the deployment
./scripts/test-functions.sh aws
./scripts/test-business-rules.sh aws
```

---

**Coming up in Part 3:** The same business logic will run on Azure Functions with Cosmos DB. Three clouds, one codebase, infinite possibilities.

*Follow me to get notified when Part 3 drops! Have questions about the AWS implementation? Drop them in the comments below.*
