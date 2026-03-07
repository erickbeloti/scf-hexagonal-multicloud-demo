terraform {
  required_version = ">= 1.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}

provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
  subscription_id = var.subscription_id
}

# Random suffix for unique resource names
resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

# Resource Group
resource "azurerm_resource_group" "tasks" {
  name     = "rg-tasks-${var.environment}"
  location = var.location

  tags = {
    Environment = var.environment
    Project     = "task-management"
    ManagedBy   = "terraform"
  }
}

# =============================================================================
# COSMOS DB
# =============================================================================

# Cosmos DB Account (Serverless mode for cost efficiency in dev)
resource "azurerm_cosmosdb_account" "tasks" {
  name                = "cosmos-tasks-${var.environment}-${random_string.suffix.result}"
  location            = azurerm_resource_group.tasks.location
  resource_group_name = azurerm_resource_group.tasks.name
  offer_type          = "Standard"
  kind                = "GlobalDocumentDB"

  automatic_failover_enabled = false

  # Serverless mode for reduced costs in dev/staging
  capabilities {
    name = "EnableServerless"
  }

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = azurerm_resource_group.tasks.location
    failover_priority = 0
  }

  tags = {
    Environment = var.environment
    Project     = "task-management"
  }
}

# Cosmos DB SQL Database
resource "azurerm_cosmosdb_sql_database" "tasks" {
  name                = "tasks-db"
  resource_group_name = azurerm_resource_group.tasks.name
  account_name        = azurerm_cosmosdb_account.tasks.name
}

# Cosmos DB Container with partition key on userId
resource "azurerm_cosmosdb_sql_container" "tasks" {
  name                  = "tasks"
  resource_group_name   = azurerm_resource_group.tasks.name
  account_name          = azurerm_cosmosdb_account.tasks.name
  database_name         = azurerm_cosmosdb_sql_database.tasks.name
  partition_key_paths   = ["/userId"]
  partition_key_version = 2

  indexing_policy {
    indexing_mode = "consistent"

    included_path {
      path = "/*"
    }

    # Composite indexes for frequent queries
    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/createdAt"
        order = "Descending"
      }
    }

    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/status"
        order = "Ascending"
      }
    }

    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/priority"
        order = "Ascending"
      }
      index {
        path  = "/createdAt"
        order = "Ascending"
      }
    }

    composite_index {
      index {
        path  = "/userId"
        order = "Ascending"
      }
      index {
        path  = "/description"
        order = "Ascending"
      }
      index {
        path  = "/createdAt"
        order = "Ascending"
      }
    }

    excluded_path {
      path = "/\"_etag\"/?"
    }
  }
}

# =============================================================================
# AZURE FUNCTIONS INFRASTRUCTURE
# =============================================================================

# Storage Account for Function App
resource "azurerm_storage_account" "functions" {
  name                            = "sttasks${var.environment}${random_string.suffix.result}"
  resource_group_name             = azurerm_resource_group.tasks.name
  location                        = azurerm_resource_group.tasks.location
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  shared_access_key_enabled       = true

  tags = {
    Environment = var.environment
    Project     = "task-management"
  }

  lifecycle {
    ignore_changes = [
      # Ignorar mudanças que podem causar problemas de refresh
      queue_properties,
      blob_properties
    ]
  }
}

# Storage Container for function deployments
resource "azurerm_storage_container" "deployments" {
  name                  = "deployments"
  storage_account_name  = azurerm_storage_account.functions.name
  container_access_type = "private"
}

# =============================================================================
# FUNCTION CODE DEPLOYMENT
# =============================================================================

# Create zip archive from the target/deploy directory (same as AWS/GCP)
data "archive_file" "function_source" {
  type        = "zip"
  source_dir  = "${path.module}/../../target/deploy"
  output_path = "${path.module}/function-source.zip"
}

# Upload the function zip to blob storage
resource "azurerm_storage_blob" "function_code" {
  name                   = "function-source-${data.archive_file.function_source.output_md5}.zip"
  storage_account_name   = azurerm_storage_account.functions.name
  storage_container_name = azurerm_storage_container.deployments.name
  type                   = "Block"
  source                 = data.archive_file.function_source.output_path
}

# Generate SAS token for the function app to access the blob
data "azurerm_storage_account_sas" "function_sas" {
  connection_string = azurerm_storage_account.functions.primary_connection_string
  https_only        = true
  signed_version    = "2022-11-02"

  resource_types {
    service   = false
    container = false
    object    = true
  }

  services {
    blob  = true
    queue = false
    table = false
    file  = false
  }

  start  = timestamp()
  expiry = timeadd(timestamp(), "8760h") # 1 year

  permissions {
    read    = true
    write   = false
    delete  = false
    list    = false
    add     = false
    create  = false
    update  = false
    process = false
    tag     = false
    filter  = false
  }
}

# App Service Plan (Consumption/Serverless)
resource "azurerm_service_plan" "functions" {
  name                = "asp-tasks-${var.environment}"
  location            = azurerm_resource_group.tasks.location
  resource_group_name = azurerm_resource_group.tasks.name
  os_type             = "Linux"
  sku_name            = "Y1" # Consumption plan

  tags = {
    Environment = var.environment
    Project     = "task-management"
  }
}

# Application Insights for monitoring
resource "azurerm_application_insights" "functions" {
  name                = "ai-tasks-${var.environment}"
  location            = azurerm_resource_group.tasks.location
  resource_group_name = azurerm_resource_group.tasks.name
  application_type    = "java"

  tags = {
    Environment = var.environment
    Project     = "task-management"
  }
}

# Linux Function App
resource "azurerm_linux_function_app" "tasks" {
  name                       = "func-tasks-${var.environment}-${random_string.suffix.result}"
  location                   = azurerm_resource_group.tasks.location
  resource_group_name        = azurerm_resource_group.tasks.name
  service_plan_id            = azurerm_service_plan.functions.id
  storage_account_name       = azurerm_storage_account.functions.name
  storage_account_access_key = azurerm_storage_account.functions.primary_access_key

  site_config {
    application_stack {
      java_version = "21"
    }

    cors {
      allowed_origins     = ["*"]
      support_credentials = false
    }

    application_insights_key               = azurerm_application_insights.functions.instrumentation_key
    application_insights_connection_string = azurerm_application_insights.functions.connection_string
  }

  app_settings = {
    # Azure Functions runtime
    FUNCTIONS_EXTENSION_VERSION = "~4"
    FUNCTIONS_WORKER_RUNTIME    = "java"

    # Deploy from package URL (the blob with SAS token)
    WEBSITE_RUN_FROM_PACKAGE = "${azurerm_storage_blob.function_code.url}${data.azurerm_storage_account_sas.function_sas.sas}"

    # Spring Boot configuration
    SPRING_PROFILES_ACTIVE = "azure"
    MAIN_CLASS             = "com.example.tasks.TaskApplication"

    # Cosmos DB configuration
    AZURE_COSMOS_URI       = azurerm_cosmosdb_account.tasks.endpoint
    AZURE_COSMOS_KEY       = azurerm_cosmosdb_account.tasks.primary_key
    AZURE_COSMOS_DATABASE  = azurerm_cosmosdb_sql_database.tasks.name

    # Application Insights
    APPLICATIONINSIGHTS_CONNECTION_STRING = azurerm_application_insights.functions.connection_string
  }

  identity {
    type = "SystemAssigned"
  }

  tags = {
    Environment = var.environment
    Project     = "task-management"
  }

  depends_on = [
    azurerm_cosmosdb_sql_container.tasks
  ]
}

