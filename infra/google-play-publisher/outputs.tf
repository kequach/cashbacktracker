output "google_play_service_account_email" {
  description = "Add this service account as a user in Google Play Console with app-level release permissions."
  value       = google_service_account.play_publisher.email
}

output "github_environment_variables" {
  description = "GitHub Environment variables needed by .github/workflows/android-release.yml."
  value = {
    GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL      = google_service_account.play_publisher.email
    GOOGLE_PLAY_WORKLOAD_IDENTITY_PROVIDER = google_iam_workload_identity_pool_provider.github_actions.name
    PLAY_PACKAGE_NAME                      = "com.cashbacktracker"
    PLAY_RELEASE_STATUS                    = "draft"
    PLAY_TRACK                             = "internal"
  }
}

output "google_play_workload_identity_provider" {
  description = "Full Workload Identity Provider resource name for google-github-actions/auth."
  value       = google_iam_workload_identity_pool_provider.github_actions.name
}

output "workload_identity_pool_name" {
  description = "Full Workload Identity Pool resource name."
  value       = google_iam_workload_identity_pool.github_actions.name
}
