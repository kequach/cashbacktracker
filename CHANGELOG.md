# Changelog

All notable changes to the [Cashback Tracker](https://github.com/kequach/cashbacktracker) are documented in this file.

---

## v0.1.0 - 2026-06-25

Initial Android app release.

### New features

- **Local cashback tracking** - native Android app for tracking cashback promotions with product name, cashback URL, redemption date range, purchase price, IBAN, device, notes, and status.
- **Three app areas** - `Eingabe` for creating cashback entries, `Daten` for all saved cashback records, and `Stammdaten` for bank accounts and devices.
- **Cashback statuses** - entries can be saved as `Geplant` or `Eingereicht`, then cycled by tapping a data-list row through `Geplant`, `Eingereicht`, and `Ueberwiesen`.
- **Local master data** - bank accounts store nickname, account holder, and IBAN; devices store name and notes.
- **CSV export** - manual unencrypted CSV export for saved cashback data, with an explicit warning before writing readable data.
- **Milestone celebrations** - optional EUR milestone animations for reimbursed totals, with default thresholds of 100, 500, and 1000 EUR.
- **Entry celebrations** - short non-blocking celebration animation when a cashback entry is created or marked as transferred.
- **Autofill suggestions** - cashback URL and product fields show recent unique entries and can autofill repeated promotions.
- **Duplicate-use warnings** - IBAN and device selectors mark previously used options for the same cashback promotion while keeping them selectable.

### URL parsing

- **User-triggered parser** - `URL analysieren` fetches HTTPS pages only after explicit user action and keeps manual editing available when parsing is incomplete.
- **Product and date extraction** - best-effort parser fills product name and date-only redemption ranges from page title, headings, body text, meta data, image labels, and embedded script data.
- **German cashback patterns** - supports common phrases such as `Aktionszeitraum`, `Kaufzeitraum`, `Teilnahmezeitraum`, `Einloeseschluss`, `Start ist am`, and `bis zum`.
- **Fallback title parsing** - when a page cannot be read, the app derives a readable product hint from official campaign URLs or DealDoktor-style slugs.

### UI

- **Jetpack Compose UI** - single-activity Material 3 app with bottom navigation.
- **Date range picker** - tapping the redemption period opens a date range picker for date-only input.
- **Scannable data list** - cashback entries are shown as a compact list with color-coded status, text status, amount, redemption range, IBAN nickname, and device.
- **Simple app icon** - adaptive launcher icon using a cashback wordplay: a dollar sign on a backspace-style arrow.

### Data and security

- **Local-only storage** - all app data stays on device by default; Android Auto Backup is disabled.
- **Room database** - structured local data for cashbacks, bank accounts, and devices.
- **Encrypted sensitive fields** - IBANs, account holder names, device notes, and cashback notes are encrypted before Room persistence using Android Keystore-backed AES-GCM.
- **Reset baseline schema** - current database structure is version `1`, with destructive migration enabled during early development.

### Build and release

- **Central app versioning** - `VERSION_NAME` and `VERSION_CODE` live in `gradle.properties`.
- **Debug CI workflow** - GitHub Actions builds the debug APK, runs unit tests, runs Android lint, validates version metadata, and uploads workflow artifacts.
- **Release workflow** - publishing a GitHub Release builds a signed release APK, validates that the tag matches `v<VERSION_NAME>`, and attaches the APK to the release.
- **Signing via secrets** - release signing is configured through GitHub Secrets and environment variables; keystores and passwords are not committed.

### Code quality

- **Modern Android stack** - Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3, Room, DataStore, coroutines, Flow, and jsoup.
- **Focused parser tests** - regression tests cover DealDoktor-style pages, official campaign domains, short-year date ranges, ISO date ranges, embedded script data, and struck-through outdated dates.
- **Guardrails** - repository-level `AGENTS.md` documents product constraints, Android stack decisions, local data rules, security expectations, and verification commands.

