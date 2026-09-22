# Repository instructions

## Scope and sources

- Read `README.md`, `docs/requirements.md`, `docs/architecture.md`, and
  `docs/implementation-plan.md` before editing.
- Work in the Java 17 project under `java/`; preserve the other language examples.
- Implement only the author-selected step. Keep characterization, refactoring,
  persistence, and HTTP work in separate meaningful commits.
- The original assignment is preserved in README. Label solution assumptions
  explicitly and explain them in code comments where they affect behavior.

## Compatibility invariants

- Do not change `java/src/main/java/org/ikigaidigital/TimeDeposit.java`.
- Preserve public `void updateBalance(List<TimeDeposit>)` and no-argument
  construction of `TimeDepositCalculator`.
- Before calculator refactoring, add and run characterization tests against its
  unchanged implementation. The existing `assertThat(1).isEqualTo(1)` is not
  regression protection.
- Preserve exact plan strings, unknown-plan behavior, day boundaries, batch
  iteration, repeated-call behavior, and legacy floating-point operation order.
- Keep the legacy `new BigDecimal(interest)` rounding. Do not substitute
  `BigDecimal.valueOf(interest)` or round the final balance to two decimals.
- Do not advance days or invent withdrawal processing, renewal, or termination.
- Expose only the two business operations specified in the requirements.

## Implementation and checks

- Keep domain calculation independent of HTTP, SQL, and framework annotations.
- Use separate HTTP and persistence models where required by the shared class.
- Test public behavior with fixed expected outputs established from the original
  implementation; do not compute expectations by calling the implementation
  under test or duplicating its algorithm inside assertions.
- For new features, write focused tests with the smallest useful behavior slice.
- Use real PostgreSQL Testcontainers tests for mapping, rollback, and locks.
- Missing Docker is an unexecuted/failed integration check, never a pass.
- At setup time there is no Maven Wrapper. The first build step adds it. Thereafter
  run `./mvnw test` for unit tests and `./mvnw verify` for full verification from
  `java/` (Windows: `mvnw.cmd`). Do not claim these commands work before setup.
- Inspect `git diff --check`, staged changes, and relevant checks before a commit.
- Report what ran, the results, and remaining limitations. Update
  `docs/ai-workflow.md` with actual assistance and evidence for the completed step.

## Git and handoff

- Preserve unrelated author changes. Never use destructive reset/clean commands
  to make tests pass or simplify a commit.
- The personal fork is the publication target. Do not push, open a branch, or
  create a pull request in `ikigai-digital/time-deposit-take-home-kata`.
- Do not commit credentials, personal correspondence, private recruiting
  feedback, local absolute paths, or unrelated global assistant configuration.
- At a staged handoff, summarize the change, checks, and next proposed step.
  Repository instructions describe workflow; they do not authorize extra tasks,
  submission email, deployment, or changes to other repositories.
