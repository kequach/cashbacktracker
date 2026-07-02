# Changelog

All notable changes to the [Cashback Tracker](https://github.com/kequach/cashbacktracker) are documented in this file.

---

## v0.2.0 - 2026-06-26

### User-facing features

- **Separate purchase and payout accounts** - cashback entries now store the purchase account and payout account separately; data rows and CSV exports show separate nickname, IBAN, and account-holder columns for both account roles.
- **Duplicate-use warnings per account role** - repeated promotions mark previously used purchase accounts, payout accounts, and devices while keeping those options selectable.
- **CSV import** - the `Daten` tab can import the app's CSV backup format and recreate missing bank accounts or devices from the file.
- **Action-period markers** - the data list highlights entries that are not yet in the `Aktionszeitraum` and planned entries whose `Aktionszeitraum` has already expired.
- **Milestone progress and celebrations** - the data tab shows animated progress from the last reached milestone toward the next one, with milestone thresholds at 5, 10, 25, 50, 100, 150, 250, 500, 750, and 1000 EUR.
- **Celebration queue and sounds** - celebration cards stack with spacing instead of overlapping, share a visible border treatment, and paid cashbacks plus milestones play short original reward sounds through the Android media audio stream.
- **Cashback URL normalization** - the parser accepts cashback links without an `https://` prefix and normalizes them before parsing or saving.
- **Selection focus handling** - dropdown suggestions, account/device selections, and date-picker actions clear text input focus so the keyboard does not reopen unexpectedly.

### Build and data maintenance

- **APK size optimization** - release builds now use R8 minification and resource shrinking, unused dependencies were removed, and Material icon usage was replaced with small local vector assets.
- **Google Play release automation** - publishing a GitHub Release now verifies unsigned release artifacts, signs and attaches the APK/AAB from a protected environment, and publishes the AAB to the configured Google Play track using GitHub OIDC, Workload Identity Federation, and Play Console app-level permissions.
- **Google Play Terraform helper** - `infra/google-play-publisher/` now codifies the Google Cloud APIs, existing Play publishing service account, GitHub OIDC provider, and least-privilege impersonation binding needed by the Play release workflow.
- **Fresh local database baseline** - early-development local storage now uses a fresh `cashback-tracker-v1.db` file to avoid conflicts with older test installs that used the previous version-1 schema.

### Docs

- **English end-user README** - README now focuses on installation, first steps, privacy, CSV export, and everyday app usage.
- **Privacy policy** - a simple public privacy policy now documents the app's local-only storage, user-triggered URL analysis, CSV export behavior, and lack of analytics, ads, tracking, or cloud sync for Google Play publication.
- **Play Store listing assets** - Play Store assets now include a 1024 x 500 feature graphic, a 512 x 512 app icon, and emulator-captured phone, 7-inch tablet, and 10-inch tablet screenshot sets with milestone celebration captures.
- **Development guide** - build, CI, signing, release, and versioning details moved to `DEVELOPMENT.md`.
- **Changelog and guardrails** - documentation now describes purchase account and payout account separately.
- **Prompt-level changelog guardrail** - `AGENTS.md` now requires a changelog impact check for every prompt and a `CHANGELOG.md` update for substantive changes.
- **Changelog grouping guardrail** - `AGENTS.md` now requires current-release changelog entries to describe final behavior instead of artificial same-release improvements.
- **Codex workflow guide** - `docs/CODEX_WORKFLOW.md` now documents the plan/spec/implement/verify/review loop for agent-assisted work.
- **Project Codex agents and skill** - project-scoped review subagents and `$cashback-change-workflow` now codify fresh-context review, security review, and verification expectations.
- **Codex agent topology guidance** - workflow docs, skill instructions, and review agents now use a router-first, specialist-on-demand model with explicit agent-card contracts.
- **Codex test-quality guardrail** - workflow docs and review agents now flag over-mocked generated tests that do not prove real app behavior.
- **Codex token-aware templates** - feature request prompts now distinguish tiny AGENTS-only changes from material skill/reviewer workflows.
- **German umlauts** - German UI terms now use umlauts consistently in the app and docs.

---

## v0.1.0 - 2026-06-25

Initial Android app release.

### New features

- **Local cashback tracking** - native Android app for tracking cashback promotions with product name, cashback URL, redemption date range, purchase price, IBAN, device, notes, and status.
- **Three app areas** - `Eingabe` for creating cashback entries, `Daten` for all saved cashback records, and `Stammdaten` for bank accounts and devices.
- **Cashback statuses** - entries can be saved as `Geplant` or `Eingereicht`, then cycled by tapping a data-list row through `Geplant`, `Eingereicht`, and `Überwiesen`.
- **Local master data** - bank accounts store nickname, account holder, and IBAN; devices store name and notes.
- **CSV export** - manual unencrypted CSV export for saved cashback data, with an explicit warning before writing readable data.
- **Milestone celebrations** - optional EUR milestone animations for reimbursed totals, with default thresholds of 100, 500, and 1000 EUR.
- **Entry celebrations** - short non-blocking celebration animation when a cashback entry is created or marked as transferred.
- **Autofill suggestions** - cashback URL and product fields show recent unique entries and can autofill repeated promotions.
- **Duplicate-use warnings** - IBAN and device selectors mark previously used options for the same cashback promotion while keeping them selectable.

### URL parsing

- **User-triggered parser** - `URL analysieren` fetches HTTPS pages only after explicit user action and keeps manual editing available when parsing is incomplete.
- **Product and date extraction** - best-effort parser fills product name and date-only redemption ranges from page title, headings, body text, meta data, image labels, and embedded script data.
- **German cashback patterns** - supports common phrases such as `Aktionszeitraum`, `Kaufzeitraum`, `Teilnahmezeitraum`, `Einlöseschluss`, `Start ist am`, and `bis zum`.
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
