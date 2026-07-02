# Codex Workflow

This guide defines the default agent-assisted software engineering workflow for
Cashback Tracker. It keeps Codex fast for small fixes and disciplined for work
that can affect app behavior, data, privacy, release quality, or maintainability.

## Default Loop

1. **Scope**
   - State the goal, non-goals, constraints, and "done when" checks.
   - Mention relevant files, errors, screenshots, or failing tests when known.
   - For one-line or obvious fixes, go straight to implementation.

2. **Explore And Plan**
   - For ambiguous or multi-file changes, start in plan mode or ask Codex to
     explore before editing.
   - Let Codex read the relevant app, test, docs, and Gradle files.
   - Produce a short plan with affected files and verification commands.

3. **Use A Lightweight Spec**
   - For material work, keep a brief spec in the thread or task document:
     goal, non-goals, acceptance checks, risks, and task list.
   - This is the useful part of OpenSpec-style work: agree on behavior before
     writing code without adding ceremony to small edits.
   - If the team later adopts OpenSpec itself, map this loop to
     `explore -> propose -> apply -> archive`.

4. **Implement Narrowly**
   - Follow existing Kotlin, Compose, Room, repository, and ViewModel patterns.
   - Prefer small, reviewable changes over broad refactors.
   - Do not add dependencies, analytics, cloud sync, silent network behavior,
     profile/login features, or credential handling without explicit approval.

5. **Verify With Evidence**
   - Run the narrowest meaningful checks first, then broaden as risk grows.
   - Report exact commands and outcomes in the final response.
   - If Android SDK, emulator, network, or permissions block a check, say so.

6. **Fresh-Context Review**
   - For material changes, ask Codex to spawn `cashback-code-reviewer`.
   - For sensitive data, crypto, network parsing, permissions, CSV, backup, or
     storage changes, also spawn `cashback-security-reviewer`.
   - Treat subagent findings as review input, not as a replacement for tests.

7. **Docs And Changelog**
   - Update `CHANGELOG.md` for substantive app, parser, data, security, build,
     release, workflow, guardrail, or documentation changes.
   - Update README/development/security docs when behavior or developer
     commands change.

## When To Use Each Codex Surface

- **Prompt/thread context:** one-off constraints and task-specific decisions.
- **`AGENTS.md`:** durable repo rules, product constraints, verification, and
  changelog expectations.
- **`.codex/agents/`:** focused project subagents for review and security.
- **`.agents/skills/`:** repeatable workflows that should be reusable across
  Codex sessions.
- **`.codex/config.toml`:** repo-scoped Codex settings such as bounded subagent
  fan-out.
- **Worktrees:** parallel or background tasks where edits should not collide.

## Agent Topology Decisions

These decisions adapt multi-agent architecture principles to this repo without
adding platform complexity.

- **Router first:** the main Codex agent handles normal scoping,
  implementation, verification, and final synthesis.
- **Specialists on demand:** route material diffs to `cashback-code-reviewer`.
  Add `cashback-security-reviewer` only for sensitive data, crypto, network
  parsing, CSV import/export, permissions, backup, storage, or release-signing
  risk.
- **Supervisor only when needed:** use supervisor-style coordination only for
  multi-step work where separate specialist reviews must be sequenced or
  reconciled. Most tasks should need one main agent and at most one reviewer.
- **Repo-local graph, not decentralized mesh:** keep the workflow inside this
  repository with direct child subagents. Do not introduce A2A protocols,
  dynamic agent registries, external orchestration services, or extra MCP
  servers for routine development.
- **Agent cards as contracts:** each `.codex/agents/*.toml` file is the
  practical agent card: name, role, capabilities, trigger conditions, required
  context, and output format.
- **MCP boundary:** use MCP only when an external tool or data source is
  genuinely needed; do not use it as the normal communication layer between
  repo-local Codex agents.

## Project Subagents

Use these explicitly when the task warrants it:

```text
Spawn cashback-code-reviewer to review the current diff for correctness,
regressions, missing tests, and scope drift. Wait for the result and summarize
findings before finalizing.
```

```text
Spawn cashback-security-reviewer to review this change for sensitive data,
local-only storage, crypto, URL parsing/network behavior, CSV import/export,
permissions, logging, and documentation gaps.
```

Good review prompts name the plan, changed files, and what counts as a finding.
Ask reviewers to flag correctness, security, data-loss, and test gaps rather
than style preferences.

## Verification Matrix

Use the smallest check that proves the change, then broaden when the touched
surface is shared or risky.

- Domain/parser/util changes: focused unit tests, then `testDebugUnitTest`.
- ViewModel/state changes: focused ViewModel tests when present, then
  `testDebugUnitTest`.
- Room/data-shape changes: DAO or migration tests plus schema review.
- UI changes: build, previews/manual screenshot when available, and UI tests
  for important flows.
- Security/privacy changes: tests where practical, security subagent review,
  and docs/changelog check.
- Release/build changes: `assembleDebug`, `lintDebug`, and the relevant release
  or CI validation path.
- Test quality: prefer observable behavior over implementation call assertions.
  Mock Android/platform, clock, filesystem, or network boundaries when useful,
  but do not mock away the code path the change is meant to prove.

## Failure Patterns To Avoid

- Long kitchen-sink threads mixing unrelated tasks.
- Specs that drift away from code without verification.
- Chasing every subagent nit instead of correctness and requirement gaps.
- Trusting generated tests that only mirror the implementation.
- Over-mocked tests that would still pass if the real parser, repository,
  ViewModel, Room, import/export, encryption, or UI flow were broken.
- Updating workflow rules without updating `CHANGELOG.md`.
