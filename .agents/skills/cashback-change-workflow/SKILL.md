---
name: cashback-change-workflow
description: Use for Cashback Tracker feature work, bug fixes, refactors, parser/data/security changes, developer workflow changes, and release preparation that need the repo's plan/spec/verify/review loop.
---

# Cashback Change Workflow

Use this skill to keep material Cashback Tracker changes scoped, verified, and
reviewable.

## 1. Load The Local Contract

Before editing, read the relevant parts of:

- `AGENTS.md`
- `docs/CODEX_WORKFLOW.md`
- `DEVELOPMENT.md`
- The files directly involved in the requested change

If the request touches sensitive data, network parsing, CSV import/export,
permissions, encryption, backup, release signing, or data shape, treat it as a
security-sensitive change.

## 2. Scope Before Editing

For material changes, write a short plan in the thread:

- Goal
- Non-goals
- Files likely to change
- Acceptance checks
- Test/verification commands
- Changelog/docs impact

Ask one concise clarifying question only when a wrong assumption would create
user-visible behavior, data loss, security risk, or release risk.

## 3. Implement Narrowly

Follow existing Kotlin, Compose, Room, repository, and ViewModel patterns. Keep
changes close to the requested behavior. Do not add new dependencies, network
behavior, analytics, cloud storage, profile/login behavior, or credential
handling without explicit user approval.

## 4. Route Specialist Review Intentionally

Use a repo-local, router-first topology:

- The main Codex agent owns implementation and decides whether focused review
  is needed.
- For most material diffs, route once to `cashback-code-reviewer`.
- Add `cashback-security-reviewer` only for sensitive data, crypto, network
  parsing, CSV import/export, permissions, backup, storage, or release-signing
  risk.
- Treat each subagent TOML as the agent card: it defines the agent's role,
  capabilities, required context, and expected findings format.
- Keep subagents as direct children. Do not introduce MCP, A2A, registries, or
  extra orchestration for normal repo work.

Use supervisor-style coordination only when a task has multiple dependent
review streams that must be synthesized before finalizing.

## 5. Verify With Evidence

Run the narrowest meaningful checks first. Prefer this order:

1. Focused unit tests for the changed package or class.
2. `.\gradlew.bat testDebugUnitTest --no-configuration-cache`
3. `.\gradlew.bat assembleDebug --no-configuration-cache`
4. `.\gradlew.bat lintDebug --no-configuration-cache`
5. `.\gradlew.bat connectedDebugAndroidTest --no-configuration-cache` when a
   device/emulator is available and the touched surface warrants it.

Before treating new tests as evidence, check that they assert observable
behavior and would fail if the changed logic were broken. Use mocks for
Android/platform, clock, filesystem, or network boundaries, not for the code
path being proved.

Report the exact commands and results. If a check cannot run, explain why.

## 6. Review Before Finalizing

For material changes, spawn `cashback-code-reviewer` for a fresh-context diff
review. For security-sensitive changes, also spawn `cashback-security-reviewer`.
Fix confirmed findings, then rerun relevant checks.

Update `CHANGELOG.md` for substantive workflow, guardrail, app, data, parser,
security, build, release, or documentation changes.
