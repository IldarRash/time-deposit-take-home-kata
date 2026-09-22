# Implementation status

Last updated: 2026-09-22. This file records actual progress, not just intended scope.

| Step | Status | Evidence |
| --- | --- | --- |
| Requirements and design | Complete | `432b076`; upstream and legacy behavior documented. |
| Project workflow | Complete | `3b2d099`; project instructions and repeatable prompts. |
| Java build tooling | Complete | Maven 3.9.11 Wrapper, Java 17 release, JUnit parameters and coverage configured; baseline test ran. |
| Characterization tests | Complete | 57 scenarios passed on unchanged production code; calculator lines and branches 100%. |
| Calculation refactor | Complete | Same 57 scenarios pass; rule dispatch extracted into one small class. |
| Persistence and transactions | Complete | 11 PostgreSQL integration scenarios passed in CI run 35756519228. |
| HTTP API and OpenAPI | Complete | CI run 35756927397 passed 58 unit and 15 integration scenarios. |
| Runtime and CI | Complete | Executable JAR, seed replay, two cycles, and restart persistence passed in CI. |
| Final clean verification | Complete | Run 35757618364 at `e204f69`: 58 unit + 15 integration scenarios, coverage gate, and runtime smoke all passed. |

The author has authorized sequential implementation of the complete solution,
with small commits and publication to the personal fork. Java 17 and simplicity
are explicit choices. A single module, two HTTP operations, and small explicit
boundaries are sufficient; avoid generic frameworks and speculative features.

## Baseline

- Upstream: `c4ea3585e7dd0d4d902268cae83569ca512571b4`.
- Existing `TimeDeposit` must stay unchanged.
- Preserve formula, exact strings, eligibility, rounding, mutation, and repeated calls.
- Initial temporary probe: 16 checks passed on Java 17, including the 18.01 rounding case.

## Verification log

- Documentation setup: local links, code fences, and Git whitespace checks passed.
- Public fork and first two commit SHAs verified against GitHub.
- Java 17 compilation of the initial bounded probe passed.
- Build step: `mvnw.cmd test` on Java 17 passed (1 original test, no failures or
  skips). Its `1 == 1` assertion remains ineffective; this only verifies tooling.
- Characterization step: `mvnw.cmd clean test` passed 57 scenarios with no failures
  or skips. JaCoCo: calculator 15/15 lines and 14/14 branches; shared model 12/12
  lines. Production sources were still identical to upstream when this ran.
- Refactor step: `mvnw.cmd clean test` again passed the same 57 scenarios.
  `TimeDeposit` remains unchanged; no assertions were altered for the refactor.
- Persistence: [CI run 35756519228](https://github.com/IldarRash/time-deposit-take-home-kata/actions/runs/35756519228)
  at `3123e26` passed 57 unit scenarios and 11 PostgreSQL integration scenarios
  with no failures or skips. This includes NUMERIC precision, rollback, schema
  constraints, and observed overlapping row locks.
- API: [CI run 35756927397](https://github.com/IldarRash/time-deposit-take-home-kata/actions/runs/35756927397)
  at `d4a0337` passed 58 unit and 15 integration scenarios, including HTTP schema,
  empty state, two POST cycles, withdrawals, status codes, and contract mappings.
- Review refinement: plan strings use unbounded SQL TEXT, matching the legacy
  string contract without an invented 100-character restriction. The existing
  mixed-plan persistence scenario now includes a longer unknown plan string.
- Runtime setup: `mvnw.cmd package` passed all 58 local unit/contract scenarios
  and produced the executable Spring Boot JAR.
- Full runtime: [CI run 35757618364](https://github.com/IldarRash/time-deposit-take-home-kata/actions/runs/35757618364)
  at `e204f69` passed 58 unit scenarios, 15 PostgreSQL/HTTP integration scenarios,
  and the calculator/rules coverage gate (100% lines and branches). No tests were
  skipped. The separate smoke step started the packaged JAR against a clean
  Compose database, loaded and replayed demo data, applied two accrual cycles,
  and verified that balances and withdrawals survive an application restart.
- Docker engine named-pipe access is denied by the local sandbox; database tests
  were executed in GitHub Actions instead. Local unit tests and packaging passed;
  no local PostgreSQL execution is claimed.

## Handoff

Implementation and automated verification are complete. The remaining author
actions are to review the solution, understand the recorded tradeoffs, and submit
the public repository link. No submission email or deployment was performed.
Subsequent documentation-only commits do not change the verified application;
the repository Actions page also shows verification for the latest published head.
