resource "google_cloudfunctions2_function" "task_function" {
  name     = "task-function"
  location = var.region

  build_config {
    runtime     = "java21"
    entry_point = "org.springframework.cloud.function.adapter.gcp.GcfJarLauncher"
    source {
      storage_source {
        bucket = google_storage_bucket.function_bucket.name
        object = google_storage_bucket_object.function_archive.name
      }
    }
  }

  service_config {
    available_memory   = "512M"
    service_account_email = google_service_account.function.email
  }
}
