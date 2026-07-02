# Privacy Policy for Cashback Tracker

Last updated: 2026-07-02

Cashback Tracker is a local-first Android app for tracking cashback and
"try for free" promotions. This policy explains how the app handles data.

## Summary

- The app does not create an account.
- The app does not use advertising, analytics, tracking, or crash-reporting SDKs.
- The app does not sync data to a cloud service.
- Cashback, bank account, device, and note data is stored locally on your
  device.
- Cashback URL analysis only contacts a website when you explicitly choose to
  analyze a URL.

## Data Stored On Your Device

You can enter cashback records, promotion links, product names, date ranges,
purchase prices, bank accounts, devices, and notes. This data is stored locally
in the app's private storage on your device.

Sensitive fields such as IBANs, account holder names, device notes, and
cashback notes are encrypted before they are stored in the local database.

The developer does not receive, collect, or store this app data on a server.

## Cashback URL Analysis

If you tap `URL analysieren`, the app fetches the cashback promotion page for
the URL you entered so it can try to prefill fields such as the product name or
promotion date range.

This network request is only made after your explicit action. The contacted
website and network providers may receive technical request information such as
your IP address and the requested URL. Cashback Tracker does not send this data
to the developer.

## CSV Export And Import

The app can export your data to a CSV file when you explicitly choose to export
it. CSV exports are not encrypted and may contain readable cashback details,
IBANs, account holder names, device information, and notes.

You are responsible for storing, sharing, or deleting exported CSV files safely.

The app can also import CSV files that you select. Imported data is processed
locally on your device.

## Data Sharing

Cashback Tracker does not sell, share, or transfer your app data to the
developer, advertisers, analytics providers, or other third parties.

Data may leave your device only through actions you choose, such as:

- analyzing a cashback URL,
- exporting a CSV file,
- sharing or backing up exported files outside the app.

## Data Retention And Deletion

App data remains on your device until you edit it, delete it, clear the app's
storage, or uninstall the app.

Android Auto Backup is disabled for this app. If you export CSV backups, those
files remain wherever you save them until you delete them.

## Children

Cashback Tracker is not designed for children and is not directed to children.

## Changes To This Policy

This policy may be updated when the app's behavior changes. The latest version
will be published in this repository.

## Contact

For privacy questions or requests, use the developer contact shown on the
Google Play listing or open an issue at:

https://github.com/kequach/cashbacktracker/issues
