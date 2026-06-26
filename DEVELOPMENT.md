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

Release builds run R8 minification and Android resource shrinking. The local
unsigned release APK produced during verification was about 1.65 MB; exact size
varies with dependency and toolchain versions.

Without release signing secrets this creates:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
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
5. Let the release workflow build and attach the signed APK.

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
  - validates signing secrets
  - builds a signed release APK
  - uploads the APK to the GitHub Release and as a workflow artifact

Generated APK files should not be committed to git.

## Release Signing

Release signing is configured through environment variables so secrets do not
need to live in the repository.

Required GitHub repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: release keystore password.
- `ANDROID_KEY_ALIAS`: key alias inside the keystore.
- `ANDROID_KEY_PASSWORD`: password for the key alias.

Create a local release keystore if needed:

```powershell
keytool -genkeypair `
  -v `
  -keystore cashback-release.jks `
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
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cashback-release.jks")) | Set-Clipboard
```

Keep the keystore private and backed up. Files ending in `.jks` are ignored by
git and must not be committed.

## Production Distribution

For direct APK distribution, attaching a signed APK to a GitHub Release is a
common approach.

For Google Play distribution, prefer building a signed Android App Bundle with
`bundleRelease` and using Play App Signing.

## Local Data Notes

The app stores data locally in Room. Sensitive fields are encrypted before they
are written to the database.

During early development the current Room schema is treated as the first
baseline schema. The local database file is `cashback-tracker-v1.db`; older test
installs using earlier database shapes are intentionally not migrated.
