# AI-assisted development workflow

## Purpose and setup

The assignment explicitly requests AI-assisted development and a reproducible
workflow. This project uses a coding assistant for analysis, test design,
implementation proposals, and review, with executable checks as evidence.
The author remains responsible for understanding and accepting the solution.

Initial setup used the Codex desktop application, Git/GitHub tooling, PowerShell,
and JDK 17. The exact selectable model identifier and desktop build were not
captured; do not infer them from the repository. Record those values if available
in subsequent sessions. No project API key, custom agent service, or hosted
model integration is needed by the application.

Project-specific custom instructions are in [AGENTS.md](../AGENTS.md). The
workflow and prompt templates below make the project rules reusable without
copying a user's global assistant configuration. No custom agent definitions or
private skill packages are required for subsequent work in this repository.
The initial session also used locally available analysis, architecture, and
GitHub workflow guidance; the relevant project decisions are recorded here and
in the linked documents, not dependent on those local files.

Codex supports repository instructions through AGENTS.md; see the
[official instructions documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md).
For another coding assistant, explicitly provide this file and AGENTS.md as
context. Reproducibility means the inputs, constraints, checks, and decisions are
available; generated wording/code is not expected to be byte-identical.

## Repeat the workflow

1. Clone the personal fork and open its root in the coding assistant.
2. Ask it to read AGENTS.md and the requirements, architecture, and implementation
   plan. Confirm the selected step and check its stated compatibility invariants.
3. Use a single-step prompt below. Do not ask for the complete solution at once.
4. Inspect the proposed diff and run that step's checks. Record skipped or blocked
   checks separately; a generated test is not evidence until it runs.
5. Review the result, record the assistance, and create the corresponding atomic
   commit. Continue only with the next author-selected step.

The application build requires Java 17. Maven Wrapper pins Maven 3.9.11 and
checks its distribution SHA-256. Commands from `java/` are:

```text
./mvnw test
./mvnw verify
```

On Windows use `mvnw.cmd test` and `mvnw.cmd verify`. The full suite will require
a running Docker engine for PostgreSQL Testcontainers once integration tests are
added. Unit tests and the Wrapper already run; integration verification is pending.

## Reusable task prompts

These are project prompt templates for subsequent work, not a verbatim transcript
of the initial conversation.

### Characterization

> Read AGENTS.md and docs/requirements.md. Implement only the characterization
> step from docs/implementation-plan.md. Keep production Java sources unchanged.
> First enumerate the observable behavior and boundary scenarios. Establish fixed
> expected values from the unchanged calculator, write readable public-API tests,
> and run them. Include the basic balance-18 rounding case and a mixed batch where
> an unknown plan precedes an eligible one. Inspect calculator branch coverage.
> Report actual commands and results, gaps, and the proposed commit contents.

### Refactoring

> Read the project instructions and existing tests. Implement only the interest
> rule refactoring step. Preserve the shared class, public facade, exact string
> dispatch, arithmetic order, and rounding. Explain the smallest abstraction that
> supports adding a new rule. Run the unchanged characterization suite, inspect
> the diff and coverage, and report any compatibility concern before committing.

### Feature increment

> Implement only step [number] from docs/implementation-plan.md. Map the change
> to requirement and acceptance IDs, identify assumptions before coding, and add
> focused behavior tests. Keep calculations outside HTTP/persistence adapters.
> Run the relevant checks and report exact evidence; do not count skipped
> integration checks as passing. Stop at this step's handoff.

### Review

> Review the current diff against AGENTS.md and docs/requirements.md. Check for
> observable behavior changes, missing boundary assertions, monetary conversion
> drift, transaction mistakes, extra endpoints, and undocumented assumptions.
> Report concrete findings with file references and examples. Distinguish actual
> defects from preferences and tests that have not run. Do not make unrelated edits.

## Assistance and validation record

| Stage | Actual assistant contribution | Why | Evidence and limits |
| --- | --- | --- | --- |
| Setup, 2026-09-22 | Read upstream README and Java files; created the personal fork; drafted requirements, assumptions, architecture, and commit plan. | Identify compatibility obligations and bound the implementation before coding. | Source baseline `c4ea3585e7dd0d4d902268cae83569ca512571b4`; public fork parent verified. No production changes. |
| Baseline analysis, 2026-09-22 | Wrote and ran a temporary Java probe against the unchanged production classes. | Resolve the rounding ambiguity and confirm selected boundary cases before designing tests. | JDK 17 compiled the two classes plus the probe; all 16 checks passed. This was not Maven/JUnit, integration testing, or coverage measurement. See requirements for scenarios and rounding output. |
| Workflow setup, 2026-09-22 | Added project instructions, reusable prompts, and this record. | Make subsequent assistance follow the same scope and compatibility constraints. | Markdown links, fenced blocks, and Git whitespace checks passed. No custom agent runtime was installed. |
| Build tooling, 2026-09-22 | Added the pinned Maven Wrapper, Java release setting, parameterized-test dependency, and JaCoCo. | Run the exercise without an IDE-specific build setup. | Wrapper test run on Java 17 passed: one original test. This is tooling evidence, not meaningful regression protection. |

Future build, test-writing, refactoring, persistence, API, and CI assistance must
be added as those steps occur. No claim is made that the author has reviewed or
accepted every design decision, or that an independent reviewer has checked the
solution. The final submission must update this record with real implementation
and verification outcomes, including remaining limitations.
