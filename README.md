# Tasks – Portable Cloud Functions with Hexagonal Architecture

Portable Task app demonstrating the same domain and application core running across GCP (Cloud Functions + Firestore), AWS (Lambda + DynamoDB), and Azure (Functions + Cosmos DB) using Spring Cloud Function and Ports & Adapters.

## Architecture

```
com.example.tasks
├─ domain/                 // Entities, policies, exceptions
│  ├─ model/               // Task, Priority, Status
│  ├─ policy/              // TaskCreationPolicy, TaskUpdatePolicy
│  └─ error/               // DomainException, ValidationError
├─ application/
│  ├─ port/inbound/        // Use cases
│  ├─ port/outbound/       // TaskRepositoryPort, ClockPort, IdGeneratorPort
│  └─ service/             // TaskService
├─ adapters/
│  ├─ inbound/http/        // Spring Cloud Function handlers (HTTP mapping)
│  └─ outbound/
│     ├─ local/            // InMemoryTaskRepository (@Profile("local"))
│     ├─ gcp/              // FirestoreTaskRepository (@Profile("gcp"))
│     ├─ aws/              // DynamoDbTaskRepository (@Profile("aws"))
│     └─ azure/            // CosmosTaskRepository (@Profile("azure"))
└─ config/                 // Beans, Jackson, exception handler
```

### Business Rules
- A user cannot create more than 5 HIGH‑priority tasks per day.
- Description must be unique per user per day.
- A user cannot have more than 50 open tasks (status != COMPLETED).
- Priority ∈ {LOW, MEDIUM, HIGH}.
- Completed tasks cannot be updated.
- Read/Delete only by owner.

## Run locally

Prereqs: Java 21, Maven.

```
make run
```

Active profile: `local`. In‑memory repository.

### HTTP endpoints (Spring Cloud Function HTTP mapping)

Base path: `/functions` (configured via `spring.cloud.function.web.path`).

- POST `/functions/createTask`  (body: CreateTaskRequest)
- POST `/functions/updateTask?id={id}`  (body: UpdateTaskRequest)
- GET `/functions/getTaskById?id={id}`
- GET `/functions/listTasksByUser?page=0&size=20`
- DELETE `/functions/deleteTask?id={id}`

Header: `X-User-Id: <user>` is required.

### curl examples

```
# Create
curl -s -X POST localhost:8080/functions/createTask \
  -H 'Content-Type: application/json' -H 'X-User-Id: u1' \
  -d '{"description":"Buy milk","priority":"HIGH"}'

# Get
curl -s -H 'X-User-Id: u1' 'localhost:8080/functions/getTaskById?id=<id>'

# Update
curl -s -X POST 'localhost:8080/functions/updateTask?id=<id>' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u1' \
  -d '{"status":"COMPLETED"}'

# List
curl -s -H 'X-User-Id: u1' 'localhost:8080/functions/listTasksByUser?page=0&size=10'

# Delete
curl -i -X DELETE -H 'X-User-Id: u1' 'localhost:8080/functions/deleteTask?id=<id>'
```

## Switch cloud adapters

Set `SPRING_PROFILES_ACTIVE` to one of: `local`, `gcp`, `aws`, `azure`.

```
SPRING_PROFILES_ACTIVE=gcp java -jar target/tasks-0.0.1-SNAPSHOT.jar
```

## Datastore notes

- Firestore: collection `tasks`. Index on `userId`, `createdAt`, `priority`, `description`. Uniqueness enforced by query pre‑check.
- DynamoDB: table `Tasks` (PK `id`). GSI1 (`pk` = `userId#date`, `sk` = `description`) for uniqueness and daily counts; GSI2 (`userId`) for listing/open counts.
- Cosmos DB: database `tasksdb`, container `tasks` (partition key `/userId`). Queries for uniqueness and counts using `createdAt` ISO string range.

## Deploy

### GCP (Cloud Functions Gen2 HTTP)

Prereqs: Firestore (native mode), project, auth.

```
./mvnw -DskipTests package
# Deploy function using container or Java 21 runtime. Example with run service:
#gcloud functions deploy tasks-fn --gen2 --runtime=java21 --region=us-central1 \
#  --entry-point org.springframework.cloud.function.adapter.gcp.GcfJarLauncher \
#  --source=. --trigger-http --allow-unauthenticated
```

Set env: `GCP_PROJECT`.

### AWS (Lambda + API Gateway)

Package with Spring Cloud Function AWS adapter (not included for brevity). Minimal approach: build image and deploy to Lambda container images. Ensure DynamoDB table exists.

Env: `AWS_REGION`, `DDB_TABLE`.

### Azure (Functions + Cosmos DB)

Package as Functions with the Spring Cloud Function adapter (container or Java). Configure Cosmos DB and env: `AZURE_COSMOS_ENDPOINT`, `AZURE_COSMOS_KEY`, `AZURE_COSMOS_DB`, `AZURE_COSMOS_CONTAINER`.

## Tests

- JUnit 5 + AssertJ + Mockito
- Unit tests for policies, service
- Integration test with `local` profile

Run:
```
make test
```

## Known limitations
- Cloud adapters are minimal and omit infra provisioning and retries.
- Pagination for AWS/Azure is simplified.
- Auth is header‑based simulation.

## Next steps
- Add Spring Cloud Function platform‑specific adapters (AWS/Azure launchers) for direct deployment.
- Add idempotency keys for create.
- Add OpenAPI docs and better pagination.

