# Implementation status

Last updated: 2026-09-22. This file records actual progress, not just intended scope.

| Step | Status | Evidence |
| --- | --- | --- |
| Requirements and design | Complete | `432b076`; upstream and legacy behavior documented. |
| Project workflow | Complete | `3b2d099`; project instructions and repeatable prompts. |
| Java build tooling | Complete | Maven 3.9.11 Wrapper, Java 17 release, JUnit parameters and coverage configured; baseline test ran. |
| Characterization tests | Complete | 57 scenarios passed on unchanged production code; calculator lines and branches 100%. |
| Calculation refactor | Pending | Production calculator unchanged. |
| Persistence and transactions | Pending | Design only. |
| HTTP API and OpenAPI | Pending | Design only. |
| Runtime and CI | Pending | Docker unavailable to the local sandbox. |
| Final clean verification | Pending | No completion claim yet. |

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
- Docker engine named-pipe access is denied by the local sandbox; database tests
  will require an accessible Docker environment, including CI. This is not a
  database test pass or a reason to skip required integration verification.
