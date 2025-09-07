# Output values for the deployed infrastructure
output "function_url" {
  description = "URL of the deployed Cloud Function"
  value       = google_cloudfunctions2_function.task_functions.service_config[0].uri
}

output "function_name" {
  description = "Name of the deployed Cloud Function"
  value       = google_cloudfunctions2_function.task_functions.name
}

output "function_location" {
  description = "Location of the deployed Cloud Function"
  value       = google_cloudfunctions2_function.task_functions.location
}

output "project_id" {
  description = "GCP Project ID"
  value       = var.project_id
}

output "storage_bucket" {
  description = "Storage bucket for function source code"
  value       = google_storage_bucket.function_bucket.name
}

output "firestore_database" {
  description = "Firestore database name"
  value       = google_firestore_database.tasks_db.name
}

# Test endpoints for each function
output "test_endpoints" {
  description = "Test endpoints for each function"
  value = {
    create_task      = "${google_cloudfunctions2_function.task_functions.service_config[0].uri}/createTask"
    update_task      = "${google_cloudfunctions2_function.task_functions.service_config[0].uri}/updateTask"
    get_task         = "${google_cloudfunctions2_function.task_functions.service_config[0].uri}/getTaskById"
    list_tasks       = "${google_cloudfunctions2_function.task_functions.service_config[0].uri}/listTasksByUser"
    delete_task      = "${google_cloudfunctions2_function.task_functions.service_config[0].uri}/deleteTask"
  }
}

# Test commands
output "test_commands" {
  description = "Sample curl commands for testing the functions"
  value = {
    create_task = "curl -X POST '${google_cloudfunctions2_function.task_functions.service_config[0].uri}' -H 'Content-Type: application/json' -H 'function.name: createTask' -d '{\"userId\":\"user123\",\"description\":\"Test task\",\"priority\":\"HIGH\"}'"

    get_task = "curl -X POST '${google_cloudfunctions2_function.task_functions.service_config[0].uri}' -H 'Content-Type: application/json' -H 'function.name: getTaskById' -d '{\"id\":\"task-id\",\"userId\":\"user123\"}'"

    list_tasks = "curl -X POST '${google_cloudfunctions2_function.task_functions.service_config[0].uri}' -H 'Content-Type: application/json' -H 'function.name: listTasksByUser' -d '{\"userId\":\"user123\",\"page\":0,\"size\":10}'"

    update_task = "curl -X POST '${google_cloudfunctions2_function.task_functions.service_config[0].uri}' -H 'Content-Type: application/json' -H 'function.name: updateTask' -d '{\"id\":\"task-id\",\"userId\":\"user123\",\"description\":\"Updated task\",\"priority\":\"MEDIUM\",\"status\":\"COMPLETED\"}'"

    delete_task = "curl -X POST '${google_cloudfunctions2_function.task_functions.service_config[0].uri}' -H 'Content-Type: application/json' -H 'function.name: deleteTask' -d '{\"id\":\"task-id\",\"userId\":\"user123\"}'"
  }
}
