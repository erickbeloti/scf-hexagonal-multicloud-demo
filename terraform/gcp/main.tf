# Terraform configuration for deploying Task Management Functions to GCP
terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}

# Configure the Google Cloud Provider
provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required APIs
resource "google_project_service" "cloud_functions" {
  service = "cloudfunctions.googleapis.com"
}

resource "google_project_service" "cloud_build" {
  service = "cloudbuild.googleapis.com"
}

resource "google_project_service" "cloud_run" {
  service = "run.googleapis.com"
}

resource "google_project_service" "firestore" {
  service = "firestore.googleapis.com"
}

# Create Firestore database
resource "google_firestore_database" "tasks_db" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.region
  type        = "FIRESTORE_NATIVE"

  depends_on = [google_project_service.firestore]
}

# Firestore composite indexes for task queries
resource "google_firestore_index" "task_user_created_date_desc_index" {
  project    = var.project_id
  database   = google_firestore_database.tasks_db.name
  collection = "tasks"

  fields {
    field_path = "userId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "DESCENDING"
  }

  depends_on = [google_firestore_database.tasks_db]
}

resource "google_firestore_index" "task_user_description_date_index" {
  project    = var.project_id
  database   = google_firestore_database.tasks_db.name
  collection = "tasks"

  fields {
    field_path = "userId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "description"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "ASCENDING"
  }

  depends_on = [google_firestore_database.tasks_db]
}

resource "google_firestore_index" "task_user_priority_date_index" {
  project    = var.project_id
  database   = google_firestore_database.tasks_db.name
  collection = "tasks"

  fields {
    field_path = "userId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "priority"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "ASCENDING"
  }

  depends_on = [google_firestore_database.tasks_db]
}

resource "google_firestore_index" "task_user_status_index" {
  project    = var.project_id
  database   = google_firestore_database.tasks_db.name
  collection = "tasks"

  fields {
    field_path = "userId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "status"
    order      = "ASCENDING"
  }

  depends_on = [google_firestore_database.tasks_db]
}

# Create a storage bucket for function source code
resource "google_storage_bucket" "function_bucket" {
  name     = "${var.project_id}-task-functions-${var.environment}"
  location = var.region

  uniform_bucket_level_access = true

  lifecycle_rule {
    condition {
      age = 30
    }
    action {
      type = "Delete"
    }
  }
}

# Create the function source archive
data "archive_file" "function_source" {
  type        = "zip"
  source_dir  = "${path.module}/../../target/deploy"
  output_path = "${path.module}/function-source.zip"
}

# Upload the function source to Cloud Storage
resource "google_storage_bucket_object" "function_source" {
  name   = "function-source-${data.archive_file.function_source.output_md5}.zip"
  bucket = google_storage_bucket.function_bucket.name
  source = data.archive_file.function_source.output_path
}

# Create a custom service account for the Cloud Function
resource "google_service_account" "function_service_account" {
  account_id   = "task-function-sa-${var.environment}"
  display_name = "Task Management Function Service Account"
  description  = "Service account for task management Cloud Function"
}

# Grant Firestore permissions to the service account
resource "google_project_iam_member" "firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.function_service_account.email}"
}

# Grant Cloud Function invoker permissions for the service account
resource "google_project_iam_member" "function_invoker" {
  project = var.project_id
  role    = "roles/cloudfunctions.invoker"
  member  = "serviceAccount:${google_service_account.function_service_account.email}"
}

# Create Task Functions (Gen 2)
resource "google_cloudfunctions2_function" "task_functions" {
  name        = "task-management-${var.environment}"
  location    = var.region
  description = "Task Management Functions for Hexagonal Architecture Demo"

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
    max_instance_count = 10
    min_instance_count = 0
    available_memory   = var.function_memory
    timeout_seconds    = var.function_timeout
    service_account_email = google_service_account.function_service_account.email

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "gcp"
      GOOGLE_CLOUD_PROJECT   = var.project_id
    }

    ingress_settings = "ALLOW_ALL"
    all_traffic_on_latest_revision = true
  }

  depends_on = [
    google_project_service.cloud_functions,
    google_project_service.cloud_build,
    google_project_service.cloud_run,
    google_firestore_database.tasks_db,
    google_service_account.function_service_account
  ]
}

# IAM binding to allow unauthenticated invocations for testing
# For Cloud Functions Gen2, we need to use Cloud Run IAM since functions run on Cloud Run
resource "google_cloud_run_service_iam_binding" "public_access" {
  project  = var.project_id
  location = var.region
  service  = google_cloudfunctions2_function.task_functions.name
  role     = "roles/run.invoker"
  members  = ["allUsers"]
}

# Also keep the Cloud Functions IAM for completeness
resource "google_cloudfunctions2_function_iam_binding" "public_access" {
  project        = google_cloudfunctions2_function.task_functions.project
  location       = google_cloudfunctions2_function.task_functions.location
  cloud_function = google_cloudfunctions2_function.task_functions.name
  role           = "roles/cloudfunctions.invoker"
  members        = ["allUsers"]
}
