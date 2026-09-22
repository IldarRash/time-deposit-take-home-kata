# Incremental implementation plan

The current stage prepares requirements and the development workflow. Later
implementation is performed one step at a time so each commit can be discussed
and verified. Do not bundle the whole solution into the initial setup commit.

## Current state

- Personal fork created from upstream `c4ea3585e7dd0d4d902268cae83569ca512571b4`.
- Java 17 selected; requirements and a proposed design documented.
- Unchanged calculator inspected and exercised in a temporary 16-check probe.
- Production code and the original test remain unchanged.
- Committed regression tests, build wrapper, API, database, and CI are pending.

## Commit sequence

Suggested messages describe completed work only. A step can be split further
when each resulting commit remains meaningful and independently verifiable.

| Step | Suggested commit | Scope and observable result | Verification before committing |
| --- | --- | --- | --- |
| 0a | `docs: define requirements and implementation plan` | Sources, assumptions, acceptance criteria, language choice, architecture, and this sequence. | Review against upstream and unchanged code; Markdown links and diff checks. |
| 0b | `docs: add development workflow and project rules` | Repository instructions, repeatable prompts, and an honest assistance/validation record. | Every instruction is actionable and contains no private machine dependencies. |
| 1 | `build: add reproducible Java test tooling` | Maven Wrapper with pinned distribution, explicit compiler release 17, parameterized-test support, coverage reporting. | Wrapper runs on Java 17; baseline test executes. State that the original assertion is ineffective. |
| 2 | `test: characterize existing balance updates` | Tests for thresholds, exact plan names, rounding, fractional/zero balances, empty/mixed lists, unchanged fields, and repeat calls. No calculator edits. | All tests pass on baseline; inspect calculator line/branch coverage and close meaningful gaps. |
| 3 | `refactor: extract plan interest rules` | Small rule contract, plan implementations, dispatch, and common application of rounded interest. | Same tests remain green; shared class and public signature unchanged; no blanket weakening of assertions. |
| 4 | `feat: persist deposits and withdrawals` | PostgreSQL migrations, port/adapter, decimal mapping, transactional balance-update use case. | Testcontainers verifies round trips, associations, keys, batch results, rollback, and concurrent updates. Resolve the decimal experiment before dependent implementation. |
| 5 | `feat: expose deposit query and balance update API` | Two HTTP operations, DTOs, static OpenAPI contract, HTTP and end-to-end tests. | Empty/populated GET, POST 204, repeated POST, persisted results, unchanged days/withdrawals, exactly two advertised operations. |
| 6 | `build: add local runtime and continuous verification` | Pinned database Compose setup, demo seed script, CI unit/integration checks. | Clean checkout builds and runs; integration checks fail visibly if Docker is unavailable. |
| 7 | `docs: document execution and verification results` | Tested commands, Swagger import/request examples, final assumptions, assistance record, and validation results. | Follow the instructions from a clean checkout and inspect final diff/history. |

## Working agreement for every step

1. Read the current requirements and inspect Git status.
2. State the selected step and the smallest observable outcome.
3. Make that step's changes; preserve unrelated work.
4. Run checks appropriate to the change and inspect the resulting diff.
5. Explain the result and any unresolved limitation. Update the assistance record.
6. Commit the coherent change with its actual validation status. Push only to the
   personal fork when requested/authorized; never create a branch or PR upstream.
7. At a staged handoff, stop before the next implementation step until the author
   continues. A broad future implementation request can authorize multiple steps,
   but they must still remain separate commits.

## Final acceptance checklist

- [ ] R01-R11 and AC01-AC12 in the requirements document are demonstrated.
- [ ] Source compatibility and baseline rounding behavior are preserved.
- [ ] No extra business APIs, renewal rules, or speculative features were added.
- [ ] Unit tests and PostgreSQL integration tests have actually run successfully.
- [ ] Coverage results refer to the calculator/rules and are accompanied by meaningful assertions.
- [ ] Clean startup, demo seed, Swagger requests, and restart persistence are verified.
- [ ] Dependency/image versions and Maven distribution are reproducible.
- [ ] Assistant contributions and remaining limitations are accurately recorded.
- [ ] Public fork contains the intended commits; no private correspondence or credentials are committed.
- [ ] The author has reviewed the submission and sends the repository link separately.
