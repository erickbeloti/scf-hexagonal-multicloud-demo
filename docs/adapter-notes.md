# Adapter Notes

This project uses [Spring Cloud Function](https://docs.spring.io/spring-cloud-function/reference) to expose function beans.

> "Functions are discovered as beans and can be exported to various targets."  
> "For Google Cloud Functions, use `org.springframework.cloud.function.adapter.gcp.GcfJarLauncher` as the entry point."

The GCP adapter lets us deploy the same function beans to Cloud Functions Gen2 or Cloud Run. Other platform adapters (AWS `FunctionInvoker`, Azure `@FunctionName`) are omitted in this GCP-only iteration.
