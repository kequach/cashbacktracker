# Development

Technical notes for building, testing, signing, and releasing Cashback Tracker.

## Stack

- Kotlin
- Gradle Kotlin DSL
- Jetpack Compose + Material 3
- Room for structured local data
- DataStore for small settings
- Android Keystore-backed AES-GCM for sensitive-field encryption
- jsoup for user-triggered cashback page parsing

## Local Build

The project includes the Gradle wrapper. From PowerShell:

```powershell
.\gradlew.bat assembleDebug --no-configuration-cache
.\gradlew.bat testDebugUnitTest --no-configuration-cache
.\gradlew.bat lintDebug --no-configuration-cache
```

Install on a connected emulator or device with:

```powershell
.\gradlew.bat installDebug --no-configuration-cache
```

Build an unsigned release APK locally with:

```powershell
.\gradlew.bat assembleRelease --no-configuration-cache
```

Build an unsigned release Android App Bundle locally with:

```powershell
.\gradlew.bat bundleRelease --no-configuration-cache
```

Release builds run R8 minification and Android resource shrinking. The local
unsigned release APK produced during verification was about 1.65 MB; exact size
varies with dependency and toolchain versions.

Without release signing secrets this creates:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
app\build\outputs\bundle\release\app-release.aab
```

## Versioning

App version values live in `gradle.properties`:

```properties
VERSION_NAME=0.2.0
VERSION_CODE=2
```

Rules:

- Use semantic versioning for `VERSION_NAME`.
- Increment `VERSION_CODE` for every public release.
- The GitHub Release tag must match `v<VERSION_NAME>`, for example `v0.2.0`.
- Update `CHANGELOG.md` for the same version before publishing a release.

Release checklist:

1. Bump `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`.
2. Update `CHANGELOG.md`.
3. Commit and push the change.
4. Create a GitHub Release tagged `v<VERSION_NAME>`.
5. Let the release workflow build and attach the signed APK/AAB.
6. Let the same workflow publish the signed AAB to the configured Google Play
   track. The default configuration targets the internal track as a draft; set
   the `google-play` environment variables explicitly for production.

## GitHub Actions

The repository has two workflows:

- `.github/workflows/android.yml`
  - runs on pushes to `main`, pull requests, and manual dispatches
  - validates version metadata
  - builds the debug APK
  - runs JVM unit tests
  - runs Android lint
  - uploads the debug APK and lint report as workflow artifacts
- `.github/workflows/android-release.yml`
  - runs when a GitHub Release is published
  - checks out the release tag
  - validates that the release tag matches `v<VERSION_NAME>`
  - runs unit tests and lint before any signing or Play credentials are loaded
  - builds unsigned release APK and AAB artifacts
  - signs the APK and AAB in a separate protected environment job
  - uploads the APK and AAB to the GitHub Release and as workflow artifacts
  - authenticates to Google Cloud with GitHub OIDC and Workload Identity
    Federation only in the Play publishing job
  - publishes the signed AAB to the configured Google Play track

Generated APK files should not be committed to git.

## Release Signing

Release signing is configured through GitHub Environment secrets so credentials
do not need to live in the repository and are not available to the untrusted
build/test job.

Required GitHub Environment secrets in the protected `google-play` environment:

- `ANDROID_KEYSTORE_BASE64`: base64 encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: release keystore password.
- `ANDROID_KEY_ALIAS`: key alias inside the keystore.
- `ANDROID_KEY_PASSWORD`: password for the key alias.

Create a local release keystore outside the repository if needed:

```powershell
$releaseKeystorePath = "$env:USERPROFILE\cashback-release.jks"
keytool -genkeypair `
  -v `
  -keystore $releaseKeystorePath `
  -alias cashback-release `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

If you press Enter at the key password prompt, keytool usually uses the same
password as the keystore password. In that case set `ANDROID_KEY_PASSWORD` to
the same value as `ANDROID_KEYSTORE_PASSWORD`.

Create the base64 GitHub secret value from PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes($releaseKeystorePath)) | Set-Clipboard
```

Keep the keystore private, outside the checkout, and backed up. Files ending in
`.jks` are ignored by git, but `.gitignore` is only a last-resort safety net and
must not be treated as key protection.

## Google Play Publishing

Google Play publishing is triggered by the same GitHub Release workflow that
builds direct APK release artifacts. The workflow builds unsigned release
artifacts first, signs them in a protected job, then publishes the signed AAB,
because Google Play serves optimized APKs from Android App Bundles.

Manual Google Play Console steps:

1. Create a Google Play Developer account if you do not already have one.
2. Create the Cashback Tracker app in Play Console with package name
   `com.cashbacktracker`. The package name is permanent after the first upload.
3. Complete the required Play Console setup before production release:
   store listing, app category, screenshots, content rating, target audience,
   privacy policy if required, and Data safety. The Data safety answers must
   match the app behavior: local-first storage, no analytics/ad SDKs, no cloud
   sync, and user-triggered network access only for cashback URL analysis.
   Use the public repository privacy policy URL:
   `https://github.com/kequach/cashbacktracker/blob/main/PRIVACY.md`. If the
   repository ever becomes private, publish the same policy through GitHub Pages
   or another public website and update Play Console.
4. Enroll the app in Play App Signing and decide the signing path:
   - If you need existing GitHub APK installs to update seamlessly from Google
     Play, use the same app-signing certificate that signed those APKs when
     enrolling in Play App Signing.
   - If there are no installs to preserve, the simplest secure path is to let
     Google generate and protect the app-signing key, then use the repository's
     release keystore as the upload key.
   - For maximum key separation, register a separate upload key in Play Console.
     If you do this, the single signing secret set used by this workflow is no
     longer enough for both channels. Split APK app-signing and Play AAB upload
     signing before switching keys, or treat direct APK-to-Play updates as
     unsupported.
5. Create the first Play release manually in an internal testing track if the
   API upload reports that the package cannot be found. After the package exists
   and the app setup is complete, GitHub Release automation can publish future
   releases.

Manual Google Cloud steps:

1. Create or select a Google Cloud project.
2. Enable the Google Play Developer API.
3. Create a dedicated service account for Play publishing. Do not grant broad
   project roles unless another Google Cloud resource actually needs them.
4. Configure Workload Identity Federation for GitHub Actions and restrict it to
   this repository and the `google-play` GitHub environment. Include the
   release workflow/ref in the condition when practical. This avoids storing a
   long-lived service account JSON key in GitHub and prevents unrelated
   workflows from impersonating the Play publisher.
5. Grant the GitHub workload identity permission to impersonate the Play
   publishing service account.
6. In Play Console, invite the service account under Users and permissions and
   grant only the app-level release permissions needed for the selected track.

Manual GitHub setup:

1. Create a GitHub Environment named `google-play` and add required reviewers
   before using a production track or completed release status.
2. Add these variables to the `google-play` environment:
   - `GOOGLE_PLAY_WORKLOAD_IDENTITY_PROVIDER`: full Workload Identity Provider
     resource name, for example
     `projects/123456789/locations/global/workloadIdentityPools/github/providers/cashbacktracker`.
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`: service account email invited to Play
     Console.
   - `PLAY_PACKAGE_NAME`: optional, defaults to `com.cashbacktracker`.
   - `PLAY_TRACK`: optional, defaults to `internal`. Set to `production` only
     after Play setup and environment review gates are ready.
   - `PLAY_RELEASE_STATUS`: optional, defaults to `draft`. Use `completed` for
     a full release or `inProgress` for a staged rollout.
   - `PLAY_USER_FRACTION`: required only when `PLAY_RELEASE_STATUS` is
     `inProgress`, for example `0.1` for ten percent.
3. Add these signing secrets to the `google-play` environment:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`

GitHub Release behavior:

- The release tag must still match `v<VERSION_NAME>`.
- Unit tests and lint run before signing secrets, OIDC credentials, or Play
  credentials are loaded.
- The workflow attaches the signed APK and AAB to the GitHub Release.
- The workflow creates Play release notes from the GitHub Release body for
  `de-DE` and `en-US`, trimmed for Play's release-notes field.
- The workflow uploads the R8 mapping file with the AAB so Play can deobfuscate
  release stack traces.
- No service account JSON key is committed or stored as a GitHub secret; the
  workflow uses short-lived OIDC credentials.

## Production Distribution

For direct APK distribution, attaching a signed APK to a GitHub Release is a
common approach. For Google Play distribution, the release workflow builds a
release Android App Bundle with `bundleRelease`, signs it in the protected
release job, and publishes it with Play App Signing.

## Codex Workflow

Use [docs/CODEX_WORKFLOW.md](docs/CODEX_WORKFLOW.md) for agent-assisted
development in this repository.

Project-specific Codex assets live in:

- `.codex/agents/` for custom subagents such as `cashback-code-reviewer` and
  `cashback-security-reviewer`.
- `.agents/skills/cashback-change-workflow/` for the reusable change workflow
  skill.
- `AGENTS.md` for repository-wide product, security, and verification rules.

## Local Data Notes

The app stores data locally in Room. Sensitive fields are encrypted before they
are written to the database.

During early development the current Room schema is treated as the first
baseline schema. The local database file is `cashback-tracker-v1.db`; older test
installs using earlier database shapes are intentionally not migrated.
