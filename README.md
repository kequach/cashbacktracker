# Cashback Tracker

Native Android app for tracking cashback promotions locally.

## Product Notes

- Three main areas:
  - `Eingabe`: URL-first cashback entry, product name, redemption date range,
    purchase price, IBAN, device, and notes.
  - `Daten`: list of all entered cashbacks, paid status, paid-status reset,
    milestone toggle, and CSV export.
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
- Creating a cashback entry and marking an entry as paid show short celebration
  animations. Milestone celebrations remain controlled by the settings toggle.
- The URL parser runs only after the user taps `URL analysieren`. It fetches
  HTTPS pages, parses HTML with jsoup, and best-effort fills title and date-only
  redemption range fields.
- Milestone celebrations are optional. Default EUR thresholds are 100, 500, and
  1000.
- Typing a cashback link or product name shows suggestions from previously
  entered cashbacks so the same promotion can be reused with another IBAN or
  device.
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
