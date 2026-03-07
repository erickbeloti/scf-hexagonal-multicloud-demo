output "function_url" {
  description = "Azure Function App URL"
  value       = "https://${azurerm_linux_function_app.tasks.default_hostname}"
}

output "function_app_name" {
  description = "Function App name"
  value       = azurerm_linux_function_app.tasks.name
}

output "cosmos_endpoint" {
  description = "Cosmos DB endpoint"
  value       = azurerm_cosmosdb_account.tasks.endpoint
  sensitive   = true
}

output "cosmos_database" {
  description = "Cosmos DB database name"
  value       = azurerm_cosmosdb_sql_database.tasks.name
}

output "cosmos_container" {
  description = "Cosmos DB container name"
  value       = azurerm_cosmosdb_sql_container.tasks.name
}

output "resource_group" {
  description = "Resource group name"
  value       = azurerm_resource_group.tasks.name
}

output "storage_account" {
  description = "Storage account name"
  value       = azurerm_storage_account.functions.name
}

output "application_insights_name" {
  description = "Application Insights name"
  value       = azurerm_application_insights.functions.name
}

output "application_insights_instrumentation_key" {
  description = "Application Insights instrumentation key"
  value       = azurerm_application_insights.functions.instrumentation_key
  sensitive   = true
}

# Test commands
output "test_commands" {
  description = "Example curl commands for testing the functions"
  value = {
    create_task = "curl -X POST 'https://${azurerm_linux_function_app.tasks.default_hostname}/api/tasks' -H 'Content-Type: application/json' -H 'function.name: createTask' -d '{\"userId\":\"user123\",\"description\":\"Azure Cosmos DB task\",\"priority\":\"HIGH\"}'"
    list_tasks  = "curl -X POST 'https://${azurerm_linux_function_app.tasks.default_hostname}/api/tasks' -H 'Content-Type: application/json' -H 'function.name: listTasksByUser' -d '{\"userId\":\"user123\",\"page\":0,\"size\":10}'"
    get_task    = "curl -X POST 'https://${azurerm_linux_function_app.tasks.default_hostname}/api/tasks' -H 'Content-Type: application/json' -H 'function.name: getTaskById' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\"}'"
    update_task = "curl -X POST 'https://${azurerm_linux_function_app.tasks.default_hostname}/api/tasks' -H 'Content-Type: application/json' -H 'function.name: updateTask' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\",\"description\":\"Updated task\",\"priority\":\"MEDIUM\"}'"
    delete_task = "curl -X POST 'https://${azurerm_linux_function_app.tasks.default_hostname}/api/tasks' -H 'Content-Type: application/json' -H 'function.name: deleteTask' -d '{\"id\":\"TASK_ID\",\"userId\":\"user123\"}'"
  }
}

