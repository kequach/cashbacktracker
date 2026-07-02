# Google Play Publisher Infrastructure

Terraform for the Google Cloud side of the Cashback Tracker Google Play release
workflow.

This config manages:

- Required Google APIs for keyless GitHub Actions publishing.
- The Play publishing service account:
  `cbtracker-play-sa@cashback-tracker-playstore.iam.gserviceaccount.com`.
- A Workload Identity Pool and GitHub OIDC provider.
- The IAM binding that lets only the configured repository, GitHub Environment,
  and workflow impersonate the service account.

It does not manage:

- Google Play Developer account registration.
- Play Console app/listing setup.
- Play App Signing enrollment.
- Play Console user permissions for the service account.
- GitHub signing secrets.

## Prerequisites

1. Install Terraform `1.5` or newer.
2. Install and sign in with the Google Cloud CLI:

   ```powershell
   gcloud auth application-default login
   gcloud config set project cashback-tracker-playstore
   ```

3. Make sure your Google account can enable project services, manage service
   accounts, and manage Workload Identity Federation in the project.
4. Make sure the service account exists:

   ```text
   cbtracker-play-sa@cashback-tracker-playstore.iam.gserviceaccount.com
   ```

5. If `terraform plan` cannot access the Service Usage API in a brand-new
   project, enable `serviceusage.googleapis.com` once in Google Cloud Console,
   then rerun Terraform.

The Terraform import block in `main.tf` adopts that existing service account
into Terraform state on the first apply. If you change the project or service
account email, update the import block before applying.

Optional hardening: after the basic setup works, set `github_repository_id` in
`terraform.tfvars` to the numeric GitHub repository ID. The default config is
already restricted to `kequach/cashbacktracker`, but the numeric ID remains
stable even if a repository name is later reused.

## Run

From this directory:

```powershell
terraform init
terraform fmt -recursive
terraform plan
terraform apply
```

After `terraform apply`, copy the `github_environment_variables` output into
the GitHub Environment named `google-play`.

Expected output values are shaped like:

```text
GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL=cbtracker-play-sa@cashback-tracker-playstore.iam.gserviceaccount.com
GOOGLE_PLAY_WORKLOAD_IDENTITY_PROVIDER=projects/123456789/locations/global/workloadIdentityPools/github-actions/providers/cashbacktracker-release
PLAY_PACKAGE_NAME=com.cashbacktracker
PLAY_RELEASE_STATUS=draft
PLAY_TRACK=internal
```

Keep `PLAY_TRACK=internal` and `PLAY_RELEASE_STATUS=draft` until the Play
Console setup is proven. Change them in GitHub only when you are ready for a
production release.

## Remaining Manual Steps

1. In Google Play Console, open **Users and permissions**.
2. Invite:

   ```text
   cbtracker-play-sa@cashback-tracker-playstore.iam.gserviceaccount.com
   ```

3. Grant app-level release permissions for Cashback Tracker only.
4. In GitHub, create or open the `google-play` Environment.
5. Add the Terraform output variables.
6. Add the signing secrets:

   ```text
   ANDROID_KEYSTORE_BASE64
   ANDROID_KEYSTORE_PASSWORD
   ANDROID_KEY_ALIAS
   ANDROID_KEY_PASSWORD
   ```

7. Add required reviewers before changing the GitHub environment to production
   publishing settings.

## Security Notes

- No service account JSON key is created.
- No Android signing secret belongs in Terraform variables or state.
- The Workload Identity Provider rejects tokens outside:
  - repository: `kequach/cashbacktracker`
  - repository ID: optional, when `github_repository_id` is set
  - environment: `google-play`
  - workflow: `Android Release`
- The service account IAM binding uses a `principalSet` scoped to the configured
  repository or repository ID.
