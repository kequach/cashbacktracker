variable "project_id" {
  description = "Google Cloud project that owns the Play publishing service account and Workload Identity Federation setup."
  type        = string
  default     = "cashback-tracker-playstore"
}

variable "service_account_id" {
  description = "Existing or managed service account ID used by GitHub Actions to publish to Google Play."
  type        = string
  default     = "cbtracker-play-sa"
}

variable "service_account_display_name" {
  description = "Display name for the Play publishing service account."
  type        = string
  default     = "Cashback Tracker Play Publisher"
}

variable "workload_identity_pool_id" {
  description = "Workload Identity Pool ID for GitHub Actions."
  type        = string
  default     = "github-actions"
}

variable "workload_identity_provider_id" {
  description = "Workload Identity Provider ID for the Cashback Tracker release workflow."
  type        = string
  default     = "cashbacktracker-release"
}

variable "github_repository" {
  description = "GitHub repository allowed to impersonate the Play publishing service account, in owner/name form."
  type        = string
  default     = "kequach/cashbacktracker"
}

variable "github_repository_id" {
  description = "Optional numeric GitHub repository ID for stronger OIDC restriction. Leave null to restrict by owner/name only."
  type        = string
  default     = null
}

variable "github_environment" {
  description = "GitHub Environment required in the OIDC token."
  type        = string
  default     = "google-play"
}

variable "github_workflow" {
  description = "GitHub Actions workflow name required in the OIDC token."
  type        = string
  default     = "Android Release"
}
