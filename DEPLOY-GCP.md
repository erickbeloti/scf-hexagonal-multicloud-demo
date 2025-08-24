# Deploy to Google Cloud Functions Gen2

```bash
# enable Firestore API
gcloud services enable firestore.googleapis.com
# create Firestore in native mode (choose region as needed)
gcloud firestore databases create --region=us-central1 --type=native

# build the application
mvn -q clean package

# deploy function
gcloud functions deploy tasks \
  --gen2 \
  --runtime=java21 \
  --region=us-central1 \
  --entry-point=org.springframework.cloud.function.adapter.gcp.GcfJarLauncher \
  --memory=512MB \
  --source=target/tasks-0.1-gcp.jar \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=gcp,GCP_PROJECT=$GOOGLE_CLOUD_PROJECT

# invoke (replace URL with output of deployment)
curl -X POST "$FUNCTION_URL/function/createTask" \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1","description":"demo","priority":"LOW"}'
```
