resource "google_storage_bucket" "function_bucket" {
  name     = "${var.project_id}-function-code"
  location = var.region
}

resource "google_storage_bucket_object" "function_archive" {
  name   = "function.zip"
  bucket = google_storage_bucket.function_bucket.name
  source = "target/function.zip"
}
