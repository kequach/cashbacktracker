terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

provider "google" {
  project = var.project_id
}

import {
  to = google_service_account.play_publisher
  id = "projects/cashback-tracker-playstore/serviceAccounts/cbtracker-play-sa@cashback-tracker-playstore.iam.gserviceaccount.com"
}

locals {
  google_apis = toset([
    "androidpublisher.googleapis.com",
    "cloudresourcemanager.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "serviceusage.googleapis.com",
    "sts.googleapis.com",
  ])

  github_repository_principal_attribute = var.github_repository_id == null ? "repository" : "repository_id"
  github_repository_principal_value     = var.github_repository_id == null ? var.github_repository : var.github_repository_id
  github_repository_principal           = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github_actions.name}/attribute.${local.github_repository_principal_attribute}/${local.github_repository_principal_value}"

  github_oidc_assertion_conditions = concat([
    "assertion.repository == '${var.github_repository}'",
    "assertion.environment == '${var.github_environment}'",
    "assertion.workflow == '${var.github_workflow}'",
    ], var.github_repository_id == null ? [] : [
    "assertion.repository_id == '${var.github_repository_id}'",
  ])

}

resource "google_project_service" "required" {
  for_each = local.google_apis

  project = var.project_id
  service = each.key

  disable_dependent_services = false
  disable_on_destroy         = false
}

resource "google_service_account" "play_publisher" {
  account_id   = var.service_account_id
  display_name = var.service_account_display_name
  description  = "Publishes Cashback Tracker Android App Bundles to Google Play from GitHub Actions."
  project      = var.project_id
}

resource "google_iam_workload_identity_pool" "github_actions" {
  workload_identity_pool_id = var.workload_identity_pool_id
  display_name              = "GitHub Actions"
  description               = "Allows selected Cashback Tracker GitHub Actions jobs to authenticate to Google Cloud."
  disabled                  = false
  project                   = var.project_id

  depends_on = [
    google_project_service.required["iam.googleapis.com"],
  ]
}

resource "google_iam_workload_identity_pool_provider" "github_actions" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_actions.workload_identity_pool_id
  workload_identity_pool_provider_id = var.workload_identity_provider_id
  display_name                       = "Cashback Tracker GitHub"
  description                        = "Trusts only the release workflow in the protected GitHub environment."
  disabled                           = false
  project                            = var.project_id

  attribute_mapping = {
    "google.subject"          = "assertion.sub"
    "attribute.environment"   = "assertion.environment"
    "attribute.ref"           = "assertion.ref"
    "attribute.repository"    = "assertion.repository"
    "attribute.repository_id" = "assertion.repository_id"
    "attribute.workflow"      = "assertion.workflow"
  }

  attribute_condition = join(" && ", local.github_oidc_assertion_conditions)

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account_iam_member" "github_actions_workload_identity_user" {
  service_account_id = google_service_account.play_publisher.name
  role               = "roles/iam.workloadIdentityUser"
  member             = local.github_repository_principal
}
