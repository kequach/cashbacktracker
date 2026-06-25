# Cashback Tracker

Native Android app for tracking cashback promotions locally.

## Product Notes

- Three main areas:
  - `Eingabe`: URL-first cashback entry, product name, redemption date range,
    purchase price, IBAN, device, and notes.
  - `Daten`: list of all entered cashbacks, status changes, milestone toggle,
    and CSV export.
  - `Stammdaten`: bank accounts and redemption devices.
- Profile/login handling is not part of the app. There is no password generation,
  autofill, or credential provider integration.
- Data is stored locally. Android Auto Backup is disabled.
- Sensitive notes, IBANs, and account holder names are encrypted before they are
  written to Room using an Android Keystore-backed AES-GCM key.
- Manual CSV export is intentionally unencrypted and shows a warning before
  writing the file.
- Cashback entries assume 100% reimbursement, so the purchase price is also the
  expected cashback amount. There are no separate expected/result amount fields.
- Cashback status can be `Geplant`, `Eingereicht`, or `Ueberwiesen`. New entries
  can be saved directly as planned or submitted. Tapping a cashback row cycles
  through the three statuses.
- Creating a cashback entry and marking an entry as paid show short celebration
  animations. Milestone celebrations remain controlled by the settings toggle.
- The URL parser runs only after the user taps `URL analysieren`. It fetches
  HTTPS pages, parses HTML with jsoup, and best-effort fills title and date-only
  redemption range fields.
- Milestone celebrations are optional. Default EUR thresholds are 100, 500, and
  1000.
- Focusing the cashback link or product field shows the three newest unique
  previous entries; typing filters those suggestions.
- When entering a promotion that already exists, previously used IBANs and
  devices are highlighted in the selectors but remain selectable.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Room for structured local data
- DataStore for small settings
- Android Keystore for sensitive-field encryption
- jsoup for local HTML parsing after a user-triggered URL fetch

## Build

The project includes the Gradle wrapper and can be built from PowerShell with:

```powershell
.\gradlew.bat assembleDebug --no-configuration-cache
.\gradlew.bat testDebugUnitTest --no-configuration-cache
.\gradlew.bat lintDebug --no-configuration-cache
```

Install on a connected emulator or device with:

```powershell
.\gradlew.bat installDebug --no-configuration-cache
```

## Versioning

App version values live in `gradle.properties`:

```properties
VERSION_NAME=0.1.0
VERSION_CODE=1
```

Use semantic versioning for `VERSION_NAME` and increment `VERSION_CODE` for
every public release. The GitHub Release tag must match the version name with a
leading `v`, for example `v0.1.0`.

Release checklist:

1. Bump `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`.
2. Update `CHANGELOG.md` for the same version.
3. Commit and push the change.
4. Create a GitHub Release tagged `v<VERSION_NAME>`.
5. Let the release workflow build and attach the signed APK.

## GitHub Actions

The repository includes an Android CI workflow at `.github/workflows/android.yml`.
It runs on pushes to `main`, pull requests, and manual dispatches.

The workflow:

- builds the debug APK,
- runs JVM unit tests,
- runs Android lint,
- validates version metadata,
- uploads the debug APK as a workflow artifact,
- uploads the lint HTML report as a workflow artifact.

Do not commit generated APK files to the repository. Download them from the
GitHub Actions run artifacts instead.

The repository also includes `.github/workflows/android-release.yml`. It runs
when a GitHub Release is published, builds a signed release APK, and uploads it
to that GitHub Release. The release tag must match `v<VERSION_NAME>`.

Create these repository secrets before publishing a release:

- `ANDROID_KEYSTORE_BASE64`: base64 encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: release keystore password.
- `ANDROID_KEY_ALIAS`: key alias inside the keystore.
- `ANDROID_KEY_PASSWORD`: password for the key alias.

Create a base64 keystore secret from PowerShell with:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cashback-release.jks")) | Set-Clipboard
```

For direct APK distribution, attaching a signed APK to a GitHub Release is a
common approach. For Google Play distribution, prefer building a signed Android
App Bundle with `bundleRelease` and using Play App Signing.

## Production APK

Build a release APK with:

```powershell
.\gradlew.bat assembleRelease --no-configuration-cache
```

Without a release signing configuration this creates an unsigned APK at:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

For a production install or distribution, create a signed release APK in Android
Studio via `Build` -> `Generate Signed App Bundle / APK` -> `APK`, then choose or
create a private keystore. Keep the keystore and passwords backed up; losing
them prevents future updates with the same app identity.
