# scf-hexagonal-multicloud-demo

Demo Spring Cloud Function project with a hexagonal architecture and GCP Firestore adapter.

## Build

```bash
mvn -q -DskipTests package
mv target/scf-hexagonal-multicloud-demo-0.0.1-SNAPSHOT.jar target/function.jar
zip -j target/function.zip target/function.jar
```

## Local usage
Spring Cloud Function exposes endpoints under `/functions`.

```bash
# create
curl -X POST localhost:8080/functions/createTask \
  -H 'Content-Type: application/json' \
  -d '{"id":"1","title":"Test","description":"Desc"}'

# get
curl -X POST localhost:8080/functions/getTask \
  -H 'Content-Type: application/json' \
  -d '{"id":"1"}'

# update
curl -X POST localhost:8080/functions/updateTask \
  -H 'Content-Type: application/json' \
  -d '{"id":"1","title":"New","description":"Desc"}'

# delete
curl -X POST localhost:8080/functions/deleteTask \
  -H 'Content-Type: application/json' \
  -d '{"id":"1"}'
```

## Deploy with Terraform
Terraform configuration is in `infra/gcp/terraform` and expects `target/function.zip`.
