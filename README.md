# Task Management System - Hexagonal Architecture Multi-Cloud Demo

A Spring Boot serverless application demonstrating Hexagonal Architecture (Ports and Adapters) and Domain-Driven Design (DDD) principles across multiple cloud providers. Currently implemented for Google Cloud Platform with plans to expand to AWS and Azure.

## 🏗️ Architecture Overview

This project implements:
- **Hexagonal Architecture** with clear separation between domain, application, and infrastructure layers
- **Domain-Driven Design** with rich domain models and business rules
- **SOLID principles** throughout the codebase
- **Multi-cloud serverless deployment** using Spring Cloud Functions (currently GCP)
- **Comprehensive testing** including architecture validation and business rules
- **Environment-agnostic testing** supporting both local and deployed testing

### Domain Rules Implemented

**Create Rules:**
- User cannot create more than 5 high-priority tasks per day
- Task description must be unique per user per day
- User cannot have more than 50 open tasks
- Task must have valid priority (LOW, MEDIUM, HIGH)

**Read Rules:**
- User can only read their own tasks

**Update Rules:**
- Completed tasks cannot be updated

**Delete Rules:**
- User can only delete their own tasks

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Google Cloud CLI** (`gcloud`) for GCP deployment
- **Terraform** for infrastructure deployment

## 🚀 Quick Start

### 1. Build the Application

```bash
# Clone the repository
git clone <repository-url>
cd scf-hexagonal-multicloud-demo

# Build the application
mvn clean compile
```

### 2. Run Tests

```bash
# Run all unit and architecture tests (no server required)
mvn test
```

### 3. Local Development and Integration Testing

**Start the application:**
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

**Run integration tests** (in another terminal):
```bash
# Test basic CRUD functions (requires running server)
./scripts/test-functions.sh local

# Test business rules (requires running server)
./scripts/test-business-rules.sh local

# Run all integration test suites
./scripts/run-tests.sh local
```

## ☁️ GCP Deployment with Terraform

### Prerequisites

1. **Setup GCP Project**:
   ```bash
   # Login to GCP
   gcloud auth login
   gcloud auth application-default login
   
   # Set your project ID
   export GOOGLE_CLOUD_PROJECT="your-project-id"
   gcloud config set project $GOOGLE_CLOUD_PROJECT
   
   # Enable billing for your project (required)
   ```

2. **Install Terraform**: Download from https://terraform.io/downloads

### Deploy Infrastructure

```bash
# Step 1: Build the application for GCP
mvn clean package -Pgcp

# Step 2: Navigate to GCP terraform directory
cd terraform/gcp

# Step 3: Create terraform.tfvars with your project settings
cat > terraform.tfvars << EOF
project_id = "your-gcp-project-id"
region = "us-central1"
environment = "dev"
EOF

# Step 4: Initialize Terraform
terraform init

# Step 5: Plan deployment
terraform plan

# Step 6: Apply infrastructure
terraform apply
```

**Important**: Always build the application first before running Terraform. The deployment expects the JAR file to exist at `target/tasks-0.1-gcp.jar`.

## 🧪 Testing

The project includes a comprehensive testing framework that works with both local and deployed environments. **Important**: For both local and GCP testing, the target environment must be running before executing tests.

### Testing Locally

**Prerequisites**: Start the application first
```bash
# Terminal 1: Start the application (required)
mvn spring-boot:run -Dspring.profiles.active=local
```

**Then run tests** (in a separate terminal):
```bash
./scripts/test-functions.sh local         # Basic CRUD operations
./scripts/test-business-rules.sh local    # Domain business rules
./scripts/run-tests.sh local              # All test suites
```

### Testing GCP Deployment

**Prerequisites**: Deploy infrastructure first
```bash
cd terraform/gcp && terraform apply
```

**Quick Deployment Validation**:
```bash
./scripts/validate-deployment.sh         # Smoke test deployment status
```

This script performs a quick validation of your GCP deployment by:
- Checking Terraform deployment status
- Validating function URL accessibility  
- Testing basic function calls
- Verifying GCP resources (Cloud Functions, Firestore)

**Then run comprehensive tests**:
```bash
./scripts/test-functions.sh gcp           # Basic CRUD operations
./scripts/test-business-rules.sh gcp      # Domain business rules
./scripts/run-tests.sh gcp                # All test suites
```

### Auto-Detection Testing

The scripts can automatically detect which environment is available and ready:
```bash
# Automatically detects running local server OR deployed GCP function
./scripts/test-functions.sh              # Auto-detect environment
./scripts/test-business-rules.sh         # Auto-detect environment
./scripts/run-tests.sh                   # Auto-detect environment
```

**Note**: Auto-detection works by checking:
1. If local server is running (http://localhost:8080/actuator/health)
2. If GCP function is deployed (terraform output)
3. Fails if neither environment is available

### Manual Testing Examples

After deployment, get your function URL from Terraform output:

```bash
# Get the function URL
FUNCTION_URL=$(cd terraform/gcp && terraform output -raw function_url)
echo "Function URL: $FUNCTION_URL"
```

#### Create Task
```bash
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: createTask" \
  -d '{
    "userId": "user123",
    "description": "Complete project documentation",
    "priority": "HIGH"
  }'
```

#### Get Task by ID
```bash
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: getTaskById" \
  -d '{
    "id": "your-task-id-here",
    "userId": "user123"
  }'
```

#### List Tasks
```bash
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: listTasksByUser" \
  -d '{
    "userId": "user123",
    "page": 0,
    "size": 10
  }'
```

#### Update Task
```bash
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: updateTask" \
  -d '{
    "id": "your-task-id-here",
    "userId": "user123",
    "description": "Updated task description",
    "priority": "MEDIUM",
    "status": "COMPLETED"
  }'
```

#### Delete Task
```bash
curl -X POST "$FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "function.name: deleteTask" \
  -d '{
    "id": "your-task-id-here",
    "userId": "user123"
  }'
```

## 📁 Project Structure

```
├── src/
│   ├── main/java/com/example/tasks/
│   │   ├── domain/                     # Domain layer (entities, value objects)
│   │   ├── application/                # Application layer (use cases, services)
│   │   │   ├── port/                   # Port interfaces
│   │   │   └── service/                # Application services
│   │   ├── adapters/                   # Infrastructure layer
│   │   │   ├── inbound/functions/      # Inbound adapters (Cloud Functions)
│   │   │   └── outbound/               # Outbound adapters (repositories)
│   │   └── config/                     # Configuration classes
│   ├── resources/                      # Configuration files
│   └── test/                           # Tests including architecture validation
├── scripts/                           # Testing and utility scripts
│   ├── test-functions.sh              # Basic CRUD function tests
│   ├── test-business-rules.sh         # Domain business rules tests
│   ├── test-common.sh                 # Shared testing utilities
│   ├── run-tests.sh                   # Master test runner
│   └── validate-deployment.sh         # Deployment validation
├── terraform/gcp/                     # GCP Infrastructure as Code
└── target/                            # Build artifacts
```

## 🎯 Architecture Validation

The project includes comprehensive architecture tests that validate:

- **Hexagonal Architecture** compliance
- **DDD** layer separation
- **Dependency injection** patterns
- **Port-Adapter** pattern implementation
- **SOLID principles** adherence

Run architecture validation:
```bash
mvn test -Dtest="ArchitectureTest"
# or run all Maven tests (includes architecture tests)
mvn test
```

## 🔧 Development

### Development Workflow

1. **Start Local Development**:
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=local
   ```

2. **Run Tests During Development**:
   ```bash
   ./scripts/test-functions.sh local      # Test basic functions
   ./scripts/test-business-rules.sh local # Test business rules
   ```

3. **Validate Architecture**:
   ```bash
   mvn test -Dtest="ArchitectureTest"
   ```

### Adding New Features

1. **Domain First**: Start with domain entities and business rules
2. **Ports**: Define application port interfaces  
3. **Services**: Implement application services
4. **Adapters**: Create inbound and outbound adapters
5. **Tests**: Write comprehensive tests including architecture validation

### Code Quality

- Follow SOLID principles
- Maintain clear separation of concerns
- Write comprehensive tests
- Use meaningful domain language
- Validate architecture constraints

## 🛠️ Troubleshooting

### Common Issues

1. **Build Failures**: Ensure Java 21 is installed and configured
2. **Local Tests Failing**: Make sure local server is running first
3. **GCP Authentication**: Verify you're logged in with `gcloud auth list`
4. **Billing Issues**: Ensure billing is enabled for your GCP project
5. **Permission Errors**: Check that required APIs are enabled
6. **Function Routing Issues**: Verify `function.name` header is included in requests

### Getting Help

- Check local application logs during development
- Check GCP Cloud Functions logs: `gcloud functions logs read task-management-dev --region=us-central1`
- Review Terraform state: `terraform show`
- Validate architecture tests for design compliance
- Check Firestore indexes in the GCP Console

## 🧹 Cleanup

To remove all deployed resources:

```bash
cd terraform/gcp
terraform destroy
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow the established architecture patterns
4. Add comprehensive tests
5. Ensure architecture validation passes
6. Submit a pull request

---

**Note**: This is a multi-cloud demonstration project showcasing Hexagonal Architecture and DDD principles in serverless environments. Currently implemented for Google Cloud Platform, with planned expansions to AWS and Azure to demonstrate true cloud-agnostic architecture patterns.
