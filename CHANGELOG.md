# Changelog

All notable changes to the [Cashback Tracker](https://github.com/kequach/cashbacktracker) are documented in this file.

---

## v1.0.0 - 2026-07-03

### Features

- Native local-first Android app for cashback and "try for free" promotions.
- Create entries with link, product, date range, price, purchase/payout accounts, device, notes, status, duplicate-use warnings, and URL-assisted parsing.
- Manage bank accounts/devices, browse status-aware data, import/export CSV, and track milestones with optional celebrations.
- Play Store-ready privacy policy, icon, feature graphic, and screenshots.

### Technical

- **Modern Android stack** - Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3, single-activity Compose Navigation, Room, DataStore, coroutines, Flow, and jsoup.
- **Local encrypted storage** - sensitive fields such as IBANs, account holder names, device notes, and cashback notes are encrypted before Room persistence using Android Keystore-backed AES-GCM.
- **Local database baseline** - Room schema starts from baseline version 1 with the `cashback-tracker-v1.db` database name during early development.
- **Money and date modeling** - money is modeled as integer minor units plus currency, and redemption periods are stored as date-only ranges.
- **Behavior-focused tests** - parser, export, money formatting, and milestone policy tests cover important local behavior and regression cases.
- **Release build optimization** - release builds use R8 minification and Android resource shrinking, with unused dependencies removed and local vector assets replacing heavier icon dependencies.
- **Central app versioning** - the first release uses `VERSION_NAME=1.0.0` and `VERSION_CODE=1` from `gradle.properties`, and release tags must match `v<VERSION_NAME>`.
- **Android CI workflow** - GitHub Actions validates version metadata, builds the debug APK, runs JVM unit tests, runs Android lint, and uploads debug APK/lint artifacts.
- **GitHub Release workflow** - publishing a GitHub Release verifies unsigned release artifacts, signs APK/AAB artifacts from a protected environment, attaches them to the GitHub Release, and publishes the AAB to Google Play.
- **Google Play publishing security** - Play publishing uses GitHub OIDC, Workload Identity Federation, a Play Console service account, environment-scoped signing secrets, and no committed service account JSON key.
- **Play release notes generation** - Google Play "What's new" text is generated from the GitHub Release `Features` section when present and is capped at 500 characters without a trailing newline.
- **Google Play Terraform helper** - `infra/google-play-publisher/` codifies Google Cloud APIs, the existing Play publishing service account, the GitHub OIDC provider, and least-privilege impersonation binding.
- **Release signing workflow** - release signing uses GitHub Environment secrets for the keystore, passwords, and alias; keystores and generated credentials are ignored by git.
- **Privacy and store documentation** - README, development docs, privacy policy, and Play Store asset documentation describe app behavior, CSV risks, local-only data handling, setup, signing, and release steps.
- **Codex guardrails** - repository guardrails define the product contract, local data/security rules, UI expectations, testing expectations, changelog expectations, and release workflow rules.
- **Codex workflow assets** - project-scoped workflow docs, feature request template, reusable cashback change workflow skill, and focused review agents support repeatable implementation, verification, and review.
