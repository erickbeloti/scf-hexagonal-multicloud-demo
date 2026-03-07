# Building Portable Cloud Functions with Spring Cloud Function and Hexagonal Architecture — Part 3: Azure Functions Implementation

In [Part 1](./ARTICLE-GCP.md), we built a Task Management System using Spring Cloud Function and Hexagonal Architecture, deploying it to Google Cloud Functions with Firestore. In [Part 2](./ARTICLE-AWS.md), we proved the architecture by deploying the exact same domain code to AWS Lambda with DynamoDB. Now, we complete the trifecta: **the same business logic running on Azure Functions with Cosmos DB**.

No domain logic modifications. No business rule changes. Just infrastructure swapping.

This is the power of **true cloud portability**.

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

## The Azure Implementation Strategy

### What Stays the Same (Zero Changes)
✅ **Domain entities** — Task, TaskId, UserId, Priority, Status  
✅ **Business rules** — All validation logic remains identical  
✅ **Application services** — TaskService orchestration unchanged  
✅ **Use case interfaces** — Same inbound ports  
✅ **Function definitions** — Same Spring Cloud Function beans  

### What Changes (Infrastructure Only)
🔄 **Inbound adapter** — Azure Functions HTTP Trigger handler  
🔄 **Outbound adapter** — Cosmos DB replaces DynamoDB/Firestore  
🔄 **Maven configuration** — Azure dependencies and plugins  
🔄 **Infrastructure as Code** — Terraform for Azure resources  
🔄 **Configuration** — Azure-specific application properties

## Azure-Specific Implementation

### 1. Maven Dependencies

The project uses Spring Cloud Azure with a profile-based approach:

```xml
<profile>
    <id>azure</id>
    <properties>
        <spring.profiles.active>azure</spring.profiles.active>
        <spring-cloud-azure.version>5.19.0</spring-cloud-azure.version>
        <azure-functions-java-library.version>3.1.0</azure-functions-java-library.version>
        <azure-functions-maven-plugin.version>1.32.0</azure-functions-maven-plugin.version>
        <spring-boot-thin-layout.version>1.0.31.RELEASE</spring-boot-thin-layout.version>
        <functionAppName>task-management-azure</functionAppName>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.azure.spring</groupId>
                <artifactId>spring-cloud-azure-dependencies</artifactId>
                <version>${spring-cloud-azure.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-function-adapter-azure</artifactId>
        </dependency>
        <dependency>
            <groupId>com.microsoft.azure.functions</groupId>
            <artifactId>azure-functions-java-library</artifactId>
            <version>${azure-functions-java-library.version}</version>
        </dependency>
        <dependency>
            <groupId>com.azure.spring</groupId>
            <artifactId>spring-cloud-azure-starter-data-cosmos</artifactId>
        </dependency>
    </dependencies>
</profile>
```

### 2. Cosmos DB Document Mapping

Azure Cosmos DB uses a document-based model with partition keys for scalability:

```java
@Container(containerName = "tasks")
public class TaskDocument {

    @Id
    private String id;

    @PartitionKey
    private String userId;

    private String description;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;

    // Conversion methods between domain and persistence models
    public static TaskDocument fromDomain(Task task) {
        TaskDocument doc = new TaskDocument();
        doc.setId(task.getId().value());
        doc.setUserId(task.getUserId().value());
        doc.setDescription(task.getDescription());
        doc.setPriority(task.getPriority().name());
        doc.setStatus(task.getStatus().name());
        doc.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        doc.setUpdatedAt(task.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return doc;
    }

    public Task toDomain() {
        return Task.reconstitute(
            TaskId.of(this.id),
            UserId.of(this.userId),
            this.description,
            Priority.valueOf(this.priority),
            Status.valueOf(this.status),
            LocalDateTime.parse(this.createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse(this.updatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
```

Key differences from other cloud providers:
- **`@Container`** annotation maps to Cosmos DB container
- **`@PartitionKey`** defines the partition strategy (userId for data locality)
- **Spring Data Cosmos** handles the low-level API interactions

### 3. Cosmos DB Repository Implementation

```java
@Repository
@Profile("azure")
public class CosmosDBTaskRepository implements TaskRepositoryPort {

    private final TaskDocumentDao repository;

    public CosmosDBTaskRepository(TaskDocumentDao repository) {
        this.repository = repository;
    }

    @Override
    public Task save(Task task) {
        try {
            TaskDocument document = TaskDocument.fromDomain(task);
            repository.save(document);
            return task;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save task to Cosmos DB", e);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            return repository.findById(id.value())
                .map(TaskDocument::toDomain);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find task in Cosmos DB", e);
        }
    }

    @Override
    public List<Task> findByUserId(UserId userId, int page, int size) {
        try {
            return repository.findByUserId(userId.value())
                .stream()
                .sorted(Comparator.comparing(TaskDocument::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .map(TaskDocument::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to query tasks from Cosmos DB", e);
        }
    }

    @Override
    public long countHighPriorityTasksForUserOnDate(UserId userId, LocalDate date) {
        try {
            String startDate = date.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String endDate = date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return repository.findByUserIdAndPriorityAndCreatedAtBetween(
                userId.value(), "HIGH", startDate, endDate).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count high priority tasks", e);
        }
    }

    @Override
    public void deleteById(TaskId id) {
        try {
            repository.findById(id.value()).ifPresent(doc ->
                repository.deleteById(id.value(), new PartitionKey(doc.getUserId()))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete task from Cosmos DB", e);
        }
    }
}
```

Key points:
- **`@Profile("azure")`** ensures this adapter is only active on Azure
- **`PartitionKey`** is required for delete operations in Cosmos DB
- Queries leverage Cosmos DB's efficient indexing with composite indexes

### 4. Spring Data Cosmos Repository Interface

```java
@Repository
@Profile("azure")
public interface TaskDocumentDao extends CosmosRepository<TaskDocument, String> {

    List<TaskDocument> findByUserId(String userId);

    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.status = @status")
    List<TaskDocument> findByUserIdAndStatus(
        @Param("userId") String userId, @Param("status") String status);

    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.priority = @priority " +
           "AND c.createdAt >= @startDate AND c.createdAt < @endDate")
    List<TaskDocument> findByUserIdAndPriorityAndCreatedAtBetween(
        @Param("userId") String userId, @Param("priority") String priority,
        @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.description = @description " +
           "AND c.createdAt >= @startDate AND c.createdAt < @endDate")
    List<TaskDocument> findByUserIdAndDescriptionAndCreatedAtBetween(
        @Param("userId") String userId, @Param("description") String description,
        @Param("startDate") String startDate, @Param("endDate") String endDate);
}
```

### 5. Azure Functions HTTP Trigger

Unlike AWS Lambda and GCP Cloud Functions that work seamlessly with Spring Cloud Function's `FunctionInvoker`, Azure Functions requires a custom handler that bridges HTTP triggers to Spring Cloud Function beans:

```java
@Component
public class AzureFunctionHandler {

    private final Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask;
    private final Function<UpdateTaskRequest, Message<ResponseWrapper<Object>>> updateTask;
    private final Function<GetTaskRequest, Message<ResponseWrapper<Object>>> getTaskById;
    private final Function<ListTasksRequest, Message<ResponseWrapper<Object>>> listTasksByUser;
    private final Function<DeleteTaskRequest, Message<ResponseWrapper<Object>>> deleteTask;
    private final Function<Object, Message<ResponseWrapper<Object>>> defaultFunction;
    private final ObjectMapper objectMapper;

    private static final Map<String, Class<?>> FUNCTION_REQUEST_TYPES = Map.of(
        "createTask", CreateTaskRequest.class,
        "updateTask", UpdateTaskRequest.class,
        "getTaskById", GetTaskRequest.class,
        "listTasksByUser", ListTasksRequest.class,
        "deleteTask", DeleteTaskRequest.class
    );

    public AzureFunctionHandler(
            Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask,
            Function<UpdateTaskRequest, Message<ResponseWrapper<Object>>> updateTask,
            Function<GetTaskRequest, Message<ResponseWrapper<Object>>> getTaskById,
            Function<ListTasksRequest, Message<ResponseWrapper<Object>>> listTasksByUser,
            Function<DeleteTaskRequest, Message<ResponseWrapper<Object>>> deleteTask,
            Function<Object, Message<ResponseWrapper<Object>>> defaultFunction,
            ObjectMapper objectMapper) {
        this.createTask = createTask;
        this.updateTask = updateTask;
        this.getTaskById = getTaskById;
        this.listTasksByUser = listTasksByUser;
        this.deleteTask = deleteTask;
        this.defaultFunction = defaultFunction;
        this.objectMapper = objectMapper;
    }

    @FunctionName("taskRouter")
    public HttpResponseMessage route(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "tasks/{*path}"
            ) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        String functionName = request.getHeaders().getOrDefault("function.name", "defaultFunction");

        try {
            String body = request.getBody().orElse("{}");
            Message<ResponseWrapper<Object>> result = invokeFunction(functionName, body, context);
            ResponseWrapper<Object> responseWrapper = result.getPayload();

            String responseBody = objectMapper.writeValueAsString(responseWrapper);

            return request.createResponseBuilder(HttpStatus.valueOf(responseWrapper.getStatus()))
                .header("Content-Type", "application/json")
                .body(responseBody)
                .build();

        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"status\":500,\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + 
                      escapeJson(e.getMessage()) + "\"}]}")
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private Message<ResponseWrapper<Object>> invokeFunction(
            String functionName, String body, ExecutionContext context) throws Exception {

        Class<?> requestType = FUNCTION_REQUEST_TYPES.get(functionName);

        if (requestType == null) {
            return defaultFunction.apply(body);
        }

        Object requestObject = objectMapper.readValue(body, requestType);

        return switch (functionName) {
            case "createTask" -> createTask.apply((CreateTaskRequest) requestObject);
            case "updateTask" -> updateTask.apply((UpdateTaskRequest) requestObject);
            case "getTaskById" -> getTaskById.apply((GetTaskRequest) requestObject);
            case "listTasksByUser" -> listTasksByUser.apply((ListTasksRequest) requestObject);
            case "deleteTask" -> deleteTask.apply((DeleteTaskRequest) requestObject);
            default -> defaultFunction.apply(body);
        };
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
```

Key implementation details:
- **Constructor injection** of function beans from `TaskFunctions.java`
- **Manual JSON serialization** using `ObjectMapper` for proper response formatting
- **Function routing** via `function.name` header (same pattern as AWS/GCP)
- **Type-safe request handling** with explicit type mapping
- **Fallback to `defaultFunction`** for unknown function names

### 6. Application Configuration

```yaml
# application-azure.yml
azure:
  cosmos:
    uri: ${AZURE_COSMOS_URI}
    key: ${AZURE_COSMOS_KEY}
    database: ${AZURE_COSMOS_DATABASE:tasks-db}
    queryMetricsEnabled: ${AZURE_COSMOS_QUERY_METRICS:false}

spring:
  cloud:
    function:
      definition: functionRouter
    azure:
      cosmos:
        endpoint: ${AZURE_COSMOS_URI}
        key: ${AZURE_COSMOS_KEY}
        database: ${AZURE_COSMOS_DATABASE:tasks-db}

functions:
  provider: azure
  status-header: statusCode

logging:
  level:
    com.azure.cosmos: WARN
    com.example.tasks: DEBUG
```

## Infrastructure as Code: Terraform for Azure

### Main Resources

```hcl
# Cosmos DB Account (Serverless mode for cost efficiency)
resource "azurerm_cosmosdb_account" "tasks" {
  name                = "cosmos-tasks-${var.environment}-${random_string.suffix.result}"
  location            = azurerm_resource_group.tasks.location
  resource_group_name = azurerm_resource_group.tasks.name
  offer_type          = "Standard"
  kind                = "GlobalDocumentDB"

  # Serverless mode - pay per request
  capabilities {
    name = "EnableServerless"
  }

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = azurerm_resource_group.tasks.location
    failover_priority = 0
  }
}

# Cosmos DB Container with partition key and composite indexes
resource "azurerm_cosmosdb_sql_container" "tasks" {
  name                  = "tasks"
  resource_group_name   = azurerm_resource_group.tasks.name
  account_name          = azurerm_cosmosdb_account.tasks.name
  database_name         = azurerm_cosmosdb_sql_database.tasks.name
  partition_key_paths   = ["/userId"]
  partition_key_version = 2

  indexing_policy {
    indexing_mode = "consistent"

    included_path {
      path = "/*"
    }

    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/createdAt"
        order = "Descending"
      }
    }

    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/priority"
        order = "Ascending"
      }
      index {
        path  = "/createdAt"
        order = "Ascending"
      }
    }
  }
}

# Linux Function App with Java 21
resource "azurerm_linux_function_app" "tasks" {
  name                       = "func-tasks-${var.environment}-${random_string.suffix.result}"
  location                   = azurerm_resource_group.tasks.location
  resource_group_name        = azurerm_resource_group.tasks.name
  service_plan_id            = azurerm_service_plan.functions.id
  storage_account_name       = azurerm_storage_account.functions.name
  storage_account_access_key = azurerm_storage_account.functions.primary_access_key

  site_config {
    application_stack {
      java_version = "21"
    }
  }

  app_settings = {
    FUNCTIONS_EXTENSION_VERSION = "~4"
    FUNCTIONS_WORKER_RUNTIME    = "java"
    SPRING_PROFILES_ACTIVE      = "azure"
    MAIN_CLASS                  = "com.example.tasks.TaskApplication"
    AZURE_COSMOS_URI            = azurerm_cosmosdb_account.tasks.endpoint
    AZURE_COSMOS_KEY            = azurerm_cosmosdb_account.tasks.primary_key
    AZURE_COSMOS_DATABASE       = azurerm_cosmosdb_sql_database.tasks.name
  }

  identity {
    type = "SystemAssigned"
  }
}
```

## Deployment Process

### Build and Deploy
```bash
# Build Azure-specific JAR
mvn clean package -Pazure

# Deploy infrastructure
cd terraform/azure
terraform init
terraform apply

# Get Azure Function URL
terraform output function_url
```

## Testing in Action

### Function Routing
Spring Cloud Function uses a routing mechanism to direct requests:

```bash
# Create a task
curl -X POST "$FUNCTION_URL/api/tasks" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{
    "userId": "user123",
    "description": "Azure Functions task",
    "priority": "HIGH"
  }'

# List user tasks
curl -X POST "$FUNCTION_URL/api/tasks" \
  -H "Content-Type: application/json" \
  -H "function.name: listTasksByUser" \
  -d '{"userId": "user123", "page": 0, "size": 10}'
```

### Business Rules Validation
```bash
# Try to create 6th high-priority task (should fail)
curl -X POST "$FUNCTION_URL/api/tasks" \
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
The domain layer contains no Azure-specific code. Rules like "max 5 high-priority tasks per day" are expressed in pure Java.

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
Swapping between GCP Firestore, AWS DynamoDB, and Azure Cosmos DB requires only:
- New outbound adapter implementation
- Configuration change
- Zero domain logic changes

## Multi-Cloud Comparison

| Aspect | GCP | AWS | Azure |
|--------|-----|-----|-------|
| **Function Runtime** | Cloud Functions | Lambda | Functions |
| **Database** | Firestore | DynamoDB | Cosmos DB |
| **Serverless Model** | Pay-per-invocation | Pay-per-invocation | Consumption plan |
| **Java Version** | 21 | 21 | 21 |
| **Cold Start** | ~2-3s | ~1-2s | ~2-4s |
| **Domain Code** | ✅ Same | ✅ Same | ✅ Same |
| **Business Rules** | ✅ Same | ✅ Same | ✅ Same |

## The Power of True Portability

Our Azure implementation completes the trilogy and proves that **Hexagonal Architecture + Spring Cloud Function** delivers on its promise:

✅ **Zero business logic changes** — Rules work identically across all three clouds  
✅ **Minimal infrastructure code** — Only repository adapter needed per cloud  
✅ **Same development experience** — Testing and debugging remain consistent  
✅ **Risk mitigation** — True cloud portability reduces vendor lock-in  
✅ **Cost optimization** — Freedom to choose the most cost-effective provider  

## Get Started

Ready to deploy the same Task Management System to Azure Functions?

🔗 **Complete source code:** [GitHub Repository](https://github.com/erickbeloti/scf-hexagonal-multicloud-demo)

### Repository Structure — Complete Multi-Cloud
```
├── src/main/java/com/example/tasks/
│   ├── domain/                    # ✅ Unchanged across all clouds
│   ├── application/               # ✅ Unchanged across all clouds
│   └── adapters/
│       ├── inbound/
│       │   ├── functions/         # ✅ Shared Spring Cloud Functions
│       │   └── azure/             # 🔷 Azure HTTP Trigger handler
│       └── outbound/
│           ├── gcp/               # 🔵 GCP Firestore implementation
│           ├── aws/               # 🟡 AWS DynamoDB implementation
│           └── azure/             # 🔷 Azure Cosmos DB implementation
├── terraform/
│   ├── gcp/                       # 🔵 GCP infrastructure
│   ├── aws/                       # 🟡 AWS infrastructure
│   └── azure/                     # 🔷 Azure infrastructure
└── pom.xml                        # 🔄 All three profiles
```

### Quick Commands Reference
```bash
# Build for Azure
mvn clean package -Pazure

# Deploy to Azure
cd terraform/azure && terraform apply

# Test the deployment
./scripts/test-functions.sh azure
./scripts/test-business-rules.sh azure
```

---

**Three clouds, one codebase, infinite possibilities.**

*Follow me to get notified about future articles! Have questions about the Azure implementation? Drop them in the comments below.*

*⭐ Star the [GitHub repo](https://github.com/erickbeloti/scf-hexagonal-multicloud-demo) if you found this series valuable!*
