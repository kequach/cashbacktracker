# Codex Feature Request Template

Use this template when starting a new Codex thread for Cashback Tracker feature
work. Start the thread from the repository root so repo-scoped skills, agents,
and `AGENTS.md` are visible.

For token efficiency, use the smallest prompt that matches the risk. Tiny tasks
should rely on automatically loaded `AGENTS.md`; use the workflow skill for
material work where the extra context is worth it.

## Tiny Or Low-Risk Prompt

Use this for typos, small docs edits, obvious one-file fixes, or focused test
adjustments where no data, privacy, parser, storage, or release surface is
likely to change.

```text
Small fix:
<Describe the task in one or two sentences.>

Follow AGENTS.md only. Keep context narrow, read only the relevant files first,
and use rg to locate references. Do not invoke $cashback-change-workflow, read
workflow docs, or spawn subagents unless you find material risk. Run the focused
check that proves the change.
```

## Standard Feature Prompt

Use this for material feature work, nontrivial bug fixes, and changes that need
the repo's full plan/spec/verify/review loop.

```text
$cashback-change-workflow

Feature request:
Add <feature>.

Goal:
<Describe the user-visible behavior that should exist when this is done.>

Non-goals:
<List behavior, refactors, data-shape changes, parser behavior, privacy
posture, release work, or UI areas that should not be changed.>

Token mode:
- Keep context narrow; use rg and read only relevant files first.
- Do not spawn reviewers unless the diff is material.
- Use cashback-security-reviewer only when the touched surface matches its
  security/privacy scope.
- Summarize long command output instead of pasting logs.

Important constraints:
- Follow AGENTS.md and docs/CODEX_WORKFLOW.md.
- Keep the implementation narrow and consistent with existing Kotlin, Compose,
  Room, repository, ViewModel, Gradle, and test patterns.
- Preserve the local-only/privacy posture.
- Do not add analytics, cloud sync, silent network behavior, profile/login
  features, credential handling, new dependencies, MCP, A2A, or extra agents
  unless you explain why and ask first.
- Use the router-first workflow: the main Codex agent implements and only
  escalates to specialist reviewers when risk or task complexity warrants it.

Acceptance checks:
- <Concrete behavior 1>
- <Concrete behavior 2>
- <README/DEVELOPMENT/CHANGELOG expectation, if any>

Verification:
Run the narrowest meaningful checks first, then broaden according to
docs/CODEX_WORKFLOW.md. Prefer behavior-oriented tests; use mocks for platform
or external boundaries, not for the logic being proved. After implementation,
spawn the appropriate review agent before finalizing if the diff is material.
```

## Security-Sensitive Feature Prompt

Use this when the change touches sensitive data, crypto, URL parsing/network
behavior, CSV import/export, Android permissions, backup behavior, storage,
release signing, or data shape.

```text
$cashback-change-workflow

Feature request:
Add <feature>.

Goal:
<Describe the desired behavior and the affected user data, screens, import/export
flow, parser flow, or release path.>

Non-goals:
<List data fields, storage behavior, permissions, networking, backup behavior,
or release-signing behavior that should stay unchanged.>

Security-sensitive surfaces:
- <Sensitive data / crypto / URL parsing / CSV / permissions / backup / storage /
  release signing / data shape>

Token mode:
- Keep context narrow; read only the relevant app, tests, docs, and config files
  first.
- Use exactly the listed reviewers; do not spawn extra agents unless a new
  concrete risk appears.
- Summarize long command output instead of pasting logs.

Important constraints:
- Follow AGENTS.md and docs/CODEX_WORKFLOW.md.
- Preserve local-only storage unless explicitly approved otherwise.
- Do not log, expose, export, back up, or transmit sensitive data accidentally.
- Keep CSV import/export explicit and user-triggered.
- Keep URL parsing user-triggered, HTTPS-only, and safe on failure.
- Do not add analytics, cloud sync, silent network behavior, new dependencies,
  MCP, A2A, dynamic registries, or extra agents without asking first.

Acceptance checks:
- <Concrete behavior 1>
- <Concrete behavior 2>
- <Data/privacy/security check>

Verification:
Run focused tests first, then broaden to `testDebugUnitTest`, `assembleDebug`,
`lintDebug`, and instrumented tests only when the touched surface justifies it
and the environment supports it. Avoid over-mocked tests that would pass while
the real parser, repository, ViewModel, Room, import/export, encryption,
permission, backup, storage, or UI behavior is broken.

Review routing:
Spawn cashback-code-reviewer for material diffs. Also spawn
cashback-security-reviewer for this security-sensitive change. Summarize
findings and fix confirmed issues before finalizing.
```
