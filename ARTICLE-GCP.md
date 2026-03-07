# Building Portable Cloud Functions with Spring Cloud Function and Hexagonal Architecture — Part 1: GCP Implementation

Serverless functions have revolutionized how we build and deploy applications. With just a few clicks, you can deploy functions that respond to HTTP requests, process events, or integrate with messaging systems across major cloud providers. However, there's a significant challenge: **vendor lock-in**.

Each cloud provider offers its own framework, runtime, and deployment model:
- **AWS Lambda** with its proprietary handler interface
- **Azure Functions** with its specific triggers and bindings  
- **Google Cloud Functions** with its own function signatures

What if you could write your business logic once and deploy it anywhere?

## The Solution: Spring Cloud Function + Hexagonal Architecture

**Spring Cloud Function** provides a functional programming model that abstracts away cloud-specific implementations. You write pure business functions, and Spring Cloud Function handles the adaptation to different cloud runtimes.

**Hexagonal Architecture** (Ports and Adapters) takes this further by isolating your domain logic from external concerns like databases, messaging systems, and web frameworks.

**Together, they deliver:**
- ✅ **Portability** — Same code runs on GCP, AWS, and Azure
- ✅ **Testability** — Business logic can be tested in isolation
- ✅ **Maintainability** — Clear separation of concerns
- ✅ **Flexibility** — Easy to swap infrastructure components

## The Demo: Task Management System

I'll demonstrate this approach with a **Task Management System** that implements comprehensive business rules for each CRUD operation:

### Business Rules Implementation

**Create Operations:**
- Users cannot create more than 5 high-priority tasks per day
- Task descriptions must be unique per user per day  
- Users cannot have more than 50 open tasks
- Tasks must have valid priorities (LOW, MEDIUM, HIGH)

**Read Operations:**
- Users can only access their own tasks

**Update Operations:**
- Completed tasks cannot be modified

**Delete Operations:**
- Users can only delete their own tasks

These rules live in the **domain layer**, completely independent of cloud infrastructure.

## Architecture Deep Dive

### Package Structure

```
src/main/java/com/example/tasks/
├── domain/                          # Core business logic
│   ├── Task.java                   # Rich domain aggregate root
│   ├── TaskId.java                 # Value objects with proper encapsulation
│   ├── UserId.java                 
│   ├── Priority.java               # Domain enums
│   ├── Status.java                 
│   ├── TaskBusinessRules.java      # Business constants
│   ├── TaskValidationService.java  # Domain service interface
│   └── exception/                  # Domain exceptions
├── application/                     # Use cases & orchestration
│   ├── port/
│   │   ├── inbound/                # What the app offers (Use Cases)
│   │   │   ├── CreateTaskUseCase.java
│   │   │   ├── UpdateTaskUseCase.java
│   │   │   ├── GetTaskUseCase.java
│   │   │   ├── ListTasksUseCase.java
│   │   │   └── DeleteTaskUseCase.java
│   │   └── outbound/               # What the app needs (Repository)
│   │       └── TaskRepositoryPort.java
│   └── service/
│       ├── TaskService.java        # Business logic orchestration
│       └── TaskValidationServiceImpl.java
└── adapters/                        # Infrastructure concerns
    ├── inbound/
    │   └── functions/
    │       ├── TaskFunctions.java   # Spring Cloud Function HTTP adapter
    │       ├── TaskMessageRoutingCallback.java
    │       └── dto/                 # Request/Response DTOs
    └── outbound/
        └── gcp/
            └── FirestoreTaskRepository.java  # GCP-specific implementation
```

**Architecture Principle:** Dependencies point inward. The domain knows nothing about Firestore, HTTP, or GCP.

### Core Implementation Highlights

#### 1. Rich Domain Model with Value Objects
```java
/**
 * Task aggregate root with embedded business rules.
 * Implements DDD patterns with rich domain model and behavior.
 */
public class Task {
    private final TaskId id;
    private final UserId userId;
    private final String description;
    private final Priority priority;
    private final Status status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static void validateCreationRules(UserId userId, String description, Priority priority,
                                           LocalDate date, TaskValidationService validationService) {
        if (validationService.existsByUserAndDateAndDescription(userId, date, description)) {
            throw new TaskBusinessRuleException("Description must be unique per user per day");
        }

        if (priority.isHighPriority() &&
            validationService.countHighPriorityTasksForUserOnDate(userId, date) >= TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY) {
            throw new TaskBusinessRuleException("Cannot create more than " + TaskBusinessRules.MAX_HIGH_PRIORITY_TASKS_PER_DAY + " high priority tasks per day");
        }

        if (validationService.countOpenTasksForUser(userId) >= TaskBusinessRules.MAX_OPEN_TASKS_PER_USER) {
            throw new TaskBusinessRuleException("Cannot have more than " + TaskBusinessRules.MAX_OPEN_TASKS_PER_USER + " open tasks");
        }
    }

    public Task updateDescription(String newDescription, LocalDateTime updatedTime) {
        if (status == Status.COMPLETED) {
            throw new TaskBusinessRuleException("Cannot update completed task");
        }
        // Return new immutable instance
        return new Task(id, userId, validateDescription(newDescription), priority, status, createdAt, updatedTime);
    }
}
```

#### 2. Clean Use Case Interfaces
```java
public interface CreateTaskUseCase {
    Task createTask(UserId userId, String description, Priority priority);
}
```

#### 3. Application Service (Orchestration)
```java
@Service
public class TaskService implements CreateTaskUseCase, UpdateTaskUseCase, GetTaskUseCase, ListTasksUseCase, DeleteTaskUseCase {
    
    private final TaskRepositoryPort repository;
    private final TaskValidationService validationService;
    private final Clock clock;

    @Override
    public Task createTask(UserId userId, String description, Priority priority) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // Business rules validated using domain service interface
        Task.validateCreationRules(userId, description, priority, today, validationService);

        // Create and save task
        TaskId taskId = TaskId.generate();
        Task task = new Task(taskId, userId, description, priority, now);

        return repository.save(task);
    }
}
```

#### 4. Spring Cloud Function Adapter
```java
@Configuration
public class TaskFunctions {
    
    private final TaskService service;
    private final Validator validator;
    private final CloudResponseHelper responseHelper;

    @Bean
    public Function<CreateTaskRequest, Message<ResponseWrapper<Object>>> createTask() {
        return request -> {
            try {
                validateRequest(request);

                Task task = service.createTask(
                    UserId.of(request.userId()),
                    request.description(),
                    request.priority()
                );

                return createSuccessResponse(TaskDtoMapper.toDto(task), 200);
            } catch (TaskBusinessRuleException e) {
                logger.warn("Business rule violation: {}", e.getMessage());
                return createErrorResponse(422, "BUSINESS_RULE_VIOLATION", e.getMessage());
            }
        };
    }

    @Bean
    public Function<Object, Message<ResponseWrapper<Object>>> defaultFunction() {
        return x -> createErrorResponse(404, "FUNCTION_NOT_FOUND", "Function not found");
    }
}
```

#### 5. GCP-Specific Repository Implementation
```java
@Repository
@Profile("gcp")
public class FirestoreTaskRepository implements TaskRepositoryPort {
    
    private final Firestore firestore;
    private static final String COLLECTION = "tasks";

    @Override
    public Task save(Task task) {
        try {
            Map<String, Object> taskData = Map.of(
                "id", task.getId().value(),
                "userId", task.getUserId().value(),
                "description", task.getDescription(),
                "priority", task.getPriority().name(),
                "status", task.getStatus().name(),
                "createdAt", task.getCreatedAt().toString(),
                "updatedAt", task.getUpdatedAt().toString()
            );

            firestore.collection(COLLECTION).document(task.getId().value()).set(taskData).get();
            return task;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to save task", e);
        }
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        try {
            DocumentSnapshot document = firestore.collection(COLLECTION)
                .document(id.value()).get().get();

            return document.exists() ? 
                Optional.of(mapToTask(document)) : 
                Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find task", e);
        }
    }
}
```

## GCP Deployment with Infrastructure as Code

### Maven Configuration
```xml
<profiles>
    <profile>
        <id>gcp</id>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-function-adapter-gcp</artifactId>
            </dependency>
            <dependency>
                <groupId>com.google.cloud</groupId>
                <artifactId>spring-cloud-gcp-starter-firestore</artifactId>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

### Build and Deploy
```bash
# Build for GCP
mvn clean package -Pgcp

# Deploy infrastructure
cd terraform/gcp
terraform init
terraform apply
```

### Terraform Configuration
```hcl
resource "google_cloudfunctions2_function" "task_functions" {
  name        = "task-management-${var.environment}"
  location    = var.region
  description = "Task Management Functions"

  build_config {
    runtime     = "java21"
    entry_point = "org.springframework.cloud.function.adapter.gcp.GcfJarLauncher"
    source {
      storage_source {
        bucket = google_storage_bucket.function_bucket.name
        object = google_storage_bucket_object.function_source.name
      }
    }
  }

  service_config {
    environment_variables = {
      SPRING_PROFILES_ACTIVE = "gcp"
    }
  }
}
```

## Testing in Action

### Function Routing
Spring Cloud Function uses a routing mechanism to direct requests to the correct function:

```bash
# Create a task
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{
    "userId": "user123",
    "description": "Complete project documentation",
    "priority": "HIGH"
  }'

# List user tasks
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: listTasksByUser" \
  -d '{
    "userId": "user123",
    "page": 0,
    "size": 10
  }'
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
The domain layer contains no GCP-specific code. Rules like "max 5 high-priority tasks per day" are expressed in pure Java, making them:
- Easy to understand
- Simple to test
- Cloud-agnostic

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

This GCP implementation establishes our foundation. In the upcoming parts of this series:

**Part 2: AWS Implementation**
- Same domain code
- AWS Lambda adapter
- DynamoDB repository
- Terraform for AWS infrastructure

**Part 3: Azure Implementation**  
- Identical business logic
- Azure Functions adapter
- Cosmos DB repository
- ARM templates for Azure resources

**Part 4: Multi-Cloud Comparison**
- Performance analysis
- Cost comparison
- Operational considerations
- Deployment strategies

## The Power of Abstraction

By combining Spring Cloud Function with Hexagonal Architecture, we've achieved something remarkable: **true cloud portability without compromising functionality**.

Our task management system runs identically across cloud providers because:
- **Business rules live in the domain** — cloud-agnostic
- **Spring Cloud Function handles runtime adaptation** — no vendor lock-in
- **Ports and Adapters isolate infrastructure** — easy to swap components

## Get Started

Ready to explore cloud-portable serverless applications? 

🔗 **Complete source code:** [GitHub Repository](https://github.com/your-repo/scf-hexagonal-multicloud-demo)

📋 **What you'll find:**
- Full implementation with comprehensive tests
- Terraform infrastructure definitions
- Automated testing scripts
- Architecture validation tests
- Deployment documentation

⭐ **Star the repo** if you find this approach valuable!

---

**Next up:** Part 2 will show how the same business logic runs seamlessly on AWS Lambda with DynamoDB. Follow me to get notified when it drops!

*Have questions about Spring Cloud Function or Hexagonal Architecture? Drop them in the comments below.*
