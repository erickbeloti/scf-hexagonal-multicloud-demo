variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "eastus"

  validation {
    condition     = can(regex("^[a-z]+[a-z0-9]*$", var.location))
    error_message = "Location must be a valid Azure region."
  }
}

variable "subscription_id" {
  description = "Azure Subscription ID. Can be set via ARM_SUBSCRIPTION_ID env var or terraform.tfvars"
  type        = string
  default     = null # Will use az login subscription if not set
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "tasks"
}

