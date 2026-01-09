# Spring Cloud Function Multi-Cloud Demo - Hexagonal Architecture

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Function](https://img.shields.io/badge/Spring%20Cloud%20Function-4.3.0-blue.svg)](https://spring.io/projects/spring-cloud-function)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive demonstration of **true cloud portability** using Spring Cloud Function and Hexagonal Architecture. This project shows how the same business logic can run identically across multiple cloud providers with zero domain code changes.

## 🎯 Project Goals

- **Demonstrate Cloud Portability** - Same business logic runs on GCP, AWS, and Azure
- **Showcase Hexagonal Architecture** - Clean separation between domain, application, and infrastructure
- **Implement DDD Principles** - Rich domain models with embedded business rules
- **Prove SOLID Design** - Maintainable, testable, and extensible code
- **Enable Multi-Cloud Deployment** - Infrastructure as Code for each cloud provider

## 🏗️ Architecture Overview

This project implements a **Task Management System** using:

- **Hexagonal Architecture** (Ports and Adapters pattern)
- **Domain-Driven Design** with rich domain models
- **Spring Cloud Function** for cloud-agnostic serverless deployment
- **Infrastructure as Code** with Terraform
- **Comprehensive Testing** including architecture validation

### Package Structure

```
src/main/java/com/example/tasks/
├── domain/                          # Core business logic (cloud-agnostic)
│   ├── Task.java                   # Rich domain aggregate with business rules
│   ├── TaskId.java, UserId.java    # Value objects
│   ├── Priority.java, Status.java  # Domain enums
│   └── exception/                  # Domain exceptions
├── application/                     # Use cases & orchestration
│   ├── port/
│   │   ├── inbound/                # What the app offers (Use Cases)
│   │   └── outbound/               # What the app needs (Repository)
│   └── service/
│       └── TaskService.java        # Business logic orchestration
└── adapters/                        # Infrastructure concerns
    ├── inbound/
    │   └── functions/
    │       ├── TaskFunctions.java   # Spring Cloud Function HTTP adapter
    │       └── dto/                 # Request/Response DTOs
    └── outbound/
        ├── gcp/                    # Google Cloud Firestore implementation
        ├── aws/                    # AWS DynamoDB implementation
        └── local/                  # In-memory implementation for testing
```

## 💼 Business Rules Implemented

The Task Management System enforces these domain rules across all cloud providers:

### Create Rules
- ✅ User cannot create more than **5 high-priority tasks per day**
- ✅ Task description must be **unique per user per day**
- ✅ User cannot have more than **50 open tasks**
- ✅ Task must have valid priority (LOW, MEDIUM, HIGH)

### Read Rules
- ✅ User can only **read their own tasks**

### Update Rules
- ✅ **Completed tasks cannot be updated**

### Delete Rules
- ✅ User can only **delete their own tasks**

## 🌥️ Multi-Cloud Support

| Cloud Provider | Status | Runtime | Database | Infrastructure |
|---------------|--------|---------|----------|----------------|
| **Google Cloud Platform** | ✅ Implemented | Cloud Functions Gen2 | Firestore | Terraform |
| **Amazon Web Services** | ✅ Implemented | Lambda | DynamoDB | Terraform |
| **Microsoft Azure** | 🚧 Planned | Functions | Cosmos DB | Terraform |

### Cloud-Specific Implementations

#### Google Cloud Platform
- **Runtime**: Cloud Functions (2nd Gen) with Java 21
- **Database**: Firestore with automatic indexing
- **Infrastructure**: Terraform with Cloud Build integration
- **Entry Point**: `GcfJarLauncher`

#### Amazon Web Services  
- **Runtime**: Lambda with Java 21
- **Database**: DynamoDB with Global Secondary Index
- **Infrastructure**: Terraform with API Gateway integration
- **Entry Point**: `FunctionInvoker::handleRequest`

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Cloud CLI Tools**:
  - Google Cloud CLI (`gcloud`) for GCP deployment
  - AWS CLI (`aws`) for AWS deployment
- **Terraform 1.0+** for infrastructure deployment

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd scf-hexagonal-multicloud-demo
mvn clean test
```

### 2. Run Locally

```bash
# Start local server
mvn spring-boot:run -Dspring.profiles.active=local

# Test the functions
./scripts/test-functions.sh local
./scripts/test-business-rules.sh local
```

### 3. Deploy to Cloud

#### Google Cloud Platform

```bash
# Build for GCP
mvn clean package -Pgcp

# Deploy infrastructure
cd terraform/gcp
terraform init
terraform apply

# Test deployment
cd ../..
./scripts/test-functions.sh gcp
./scripts/test-business-rules.sh gcp
```

#### Amazon Web Services

```bash
# Build for AWS
mvn clean package -Paws

# Deploy infrastructure
cd terraform/aws
terraform init
terraform apply

# Test deployment
cd ../..
./scripts/test-functions.sh aws
./scripts/test-business-rules.sh aws
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Architecture Tests
```bash
mvn test -Dtest=ArchitectureTest
```

### Integration Tests (Local)
```bash
# Start application locally
mvn spring-boot:run -Dspring.profiles.active=local

# In another terminal
./scripts/test-functions.sh local
./scripts/test-business-rules.sh local
```

### End-to-End Tests (Cloud)
```bash
# Test deployed functions
./scripts/test-functions.sh gcp    # or aws
./scripts/test-business-rules.sh gcp  # or aws
```

## 📊 Testing Business Rules

The project includes comprehensive tests to validate business rules work identically across clouds:

```bash
# Test high-priority task limit (max 5 per day)
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{"userId":"user123","description":"Task 6","priority":"HIGH"}'

# Expected Response (HTTP 422):
{
  "status": 422,
  "errors": [{
    "code": "BUSINESS_RULE_VIOLATION",
    "message": "Cannot create more than 5 high priority tasks per day"
  }]
}
```

## 🔧 Development Workflows

### Adding New Cloud Provider

1. **Create outbound adapter** in `src/main/java/com/example/tasks/adapters/outbound/{provider}/`
2. **Implement** `TaskRepositoryPort` interface
3. **Add Maven profile** with provider-specific dependencies
4. **Create infrastructure** in `terraform/{provider}/`
5. **Update test scripts** to support new provider

### Local Development

```bash
# Run with in-memory repository
mvn spring-boot:run -Dspring.profiles.active=local

# Run with cloud-specific profile locally (requires setup)
mvn spring-boot:run -Dspring.profiles.active=gcp-local
mvn spring-boot:run -Dspring.profiles.active=aws-local
```

## 📁 Project Structure

```
├── src/
│   ├── main/java/com/example/tasks/
│   │   ├── domain/                 # Business logic (cloud-agnostic)
│   │   ├── application/            # Use cases and orchestration
│   │   └── adapters/              # Infrastructure implementations
│   └── test/                      # Unit and integration tests
├── terraform/
│   ├── gcp/                       # Google Cloud infrastructure
│   └── aws/                       # AWS infrastructure
├── scripts/                       # Testing and deployment scripts
├── ARTICLE-GCP.md                 # Part 1: GCP implementation guide
├── ARTICLE-AWS.md                 # Part 2: AWS implementation guide
└── README.md                      # This file
```

## 🎨 Key Features

### Cloud-Agnostic Business Logic
- Domain entities with embedded business rules
- Value objects for type safety
- Domain services for complex validations
- Zero cloud-specific dependencies in domain layer

### Infrastructure Flexibility
- Repository pattern with cloud-specific implementations
- Spring profiles for environment-specific configuration
- Infrastructure as Code for repeatable deployments
- Comprehensive test coverage across environments

### Developer Experience
- Single command deployment to any cloud
- Consistent testing across local and cloud environments
- Architecture validation to maintain clean design
- Comprehensive documentation and examples

## 🌟 Architecture Benefits

### True Portability
- **Same JAR** deploys to multiple clouds
- **Zero business logic changes** when switching providers
- **Consistent behavior** across all environments

### Maintainability
- **Clear separation of concerns** with hexagonal architecture
- **Domain-driven design** keeps business logic explicit
- **SOLID principles** make code easy to extend and modify

### Testability
- **Domain logic** can be tested in isolation
- **Infrastructure adapters** can be easily mocked
- **End-to-end tests** validate behavior across clouds

## 📚 Learning Resources

- **[Part 1: GCP Implementation](ARTICLE-GCP.md)** - Complete guide to GCP deployment
- **[Part 2: AWS Implementation](ARTICLE-AWS.md)** - AWS Lambda and DynamoDB setup
- **[GCP Intro](gcp-intro.md)** - Spring Cloud Function GCP documentation
- **[AWS Intro](aws-intro.md)** - Spring Cloud Function AWS documentation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure architecture tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🎯 Next Steps

- [ ] **Azure Functions implementation** with Cosmos DB
- [ ] **Performance benchmarking** across cloud providers
- [ ] **Cost analysis** and optimization guides
- [ ] **CI/CD pipelines** for automated deployment
- [ ] **Monitoring and observability** setup for each cloud

---

**Built with ❤️ to demonstrate the power of cloud-agnostic architecture using Spring Cloud Function and Hexagonal Architecture.**
