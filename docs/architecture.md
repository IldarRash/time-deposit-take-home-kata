# Architecture and compatibility decisions

Status: proposed implementation design for [the requirements](requirements.md).
No framework, API, database adapter, or migration has been implemented yet.

## Language and stack

Use **Java 17** in the existing `java/` Maven project. It matches the supplied
sources and minimum Java version, keeps the exercise focused on Java refactoring,
and avoids translating the compatibility baseline to another language.
Preserve the other language examples without modifying them.

Planned stack: Spring Boot for startup, HTTP and transaction wiring; Spring JDBC
for explicit SQL and mapping; PostgreSQL for persistence; Flyway for schema
migrations; JUnit 5 and AssertJ for tests; JaCoCo for domain coverage;
Testcontainers for real PostgreSQL integration tests. Add a Maven Wrapper.
Resolve and pin compatible Java-17 library versions at implementation time;
these are technology choices, not claims about a particular current release.

JDBC avoids turning the shared legacy model into a persistence entity and makes
batch locking and joins explicit. One Maven module and package boundaries are
sufficient; do not create a multi-module framework for this small application.

## Boundaries and data flow

```text
HTTP controller -> application use cases -> persistence port
                         |                       ^
                         v                       |
                  legacy calculator       PostgreSQL JDBC adapter
                         |
                  plan interest rules
```

- Keep `org.ikigaidigital.TimeDeposit` unchanged.
- Keep `org.ikigaidigital.TimeDepositCalculator` as the compatible public facade,
  including its no-argument construction and `updateBalance` method.
- Use the JDK `ToDoubleFunction<TimeDeposit>` contract for basic, student, and
  premium formulas in one small `PlanInterestRules` class. Rule selection uses
  exact strings and an explicit zero-interest fallback. This avoids a custom
  interface and three trivial classes; a future complex rule can be extracted
  into a named implementation without changing the calculator.
- Separate rule selection/raw interest from common rounding and balance mutation.
  Preserve the original arithmetic order, including accumulation from zero.
- Put use-case orchestration in `application`, HTTP DTOs/controllers in
  `adapter.in.web`, JDBC mapping in `adapter.out.persistence`, and wiring in
  `configuration`. These are planned package names, not existing folders.
- Define a persistence port around loading deposits (including a locking batch
  read), reading withdrawals, and saving balances. Keep SQL and framework
  annotations out of the calculator and plan rules.
- Own transaction scope in an infrastructure-wired application boundary so the
  read, calculation, and writes share one transaction and connection context.

## API contract

| Operation | Input | Successful response |
| --- | --- | --- |
| `GET /time-deposits` | None | 200, array of deposits with nested withdrawals |
| `POST /time-deposits/update-balances` | No body | 204, no body |

Example planned GET response:

```json
[
  {
    "id": 1,
    "planType": "basic",
    "balance": 1200.00,
    "days": 31,
    "withdrawals": [{"id": 10, "amount": 25.00, "date": "2026-01-15"}]
  }
]
```

Amounts are JSON numbers; clients must not depend on textual trailing zeros.
Use dedicated response records to add withdrawals without changing `TimeDeposit`.
Keep `planType` a string in the API schema, including unsupported legacy values.
Publish a static `java/openapi.yaml` at the API step. Its Swagger import workflow
avoids extra runtime documentation endpoints.

## Database and transactions

Use exact logical assignment names; in PostgreSQL migrations quote camel-case
identifiers such as `"timeDeposits"`, `"planType"`, and `"timeDepositId"`.
Use integer primary keys, required columns, a withdrawal foreign key, and an
index on the foreign key. Use `DATE` for withdrawal dates.

Propose unconstrained PostgreSQL `NUMERIC` for balance and amount, avoiding an
invented two-decimal storage rule. Map database decimals to legacy double values
only at the calculation boundary; map finite updated double values back through
`BigDecimal.valueOf`. That adapter conversion is separate from the legacy
`new BigDecimal(interest)` rounding and must not replace it.

Before finalizing this mapping, run a Testcontainers round-trip experiment with
half-cent cases, fractional balances, and repeated updates. Acceptance: the
reloaded finite double matches the direct legacy result and SQL does not truncate
the stored balance. Exact arbitrary-precision money arithmetic is outside the
legacy contract; document that limitation rather than claiming it is solved.

For updates, select deposits ordered by ID with `FOR UPDATE`, calculate, and
persist balances in one transaction. PostgreSQL's default READ COMMITTED is the
planned isolation level; verify lock behavior with two overlapping transactions.
Withdrawals are unchanged. No application endpoint inserts or deletes deposits.
Concurrent out-of-band database edits are outside the demonstrated guarantee.

For GET, load deposits and withdrawals with a bounded query strategy (for example,
one ordered LEFT JOIN mapped by deposit ID), avoiding one query per deposit and
keeping one statement's consistent view.

## Verification and remaining risks

1. Characterization tests first, against the unmodified public calculator.
2. Refactor with the same fixed expected outputs and branch coverage evidence.
3. Before persistence-dependent implementation, run the decimal mapping experiment.
4. Test repository mapping, SQL constraints, transaction rollback, and locks with
   real PostgreSQL. Use deterministic coordination rather than sleep-based tests.
5. Test HTTP serialization and the complete GET -> POST -> GET flow.
6. Validate the static contract, demo startup, and documented commands from a
   clean checkout; pin the database image used by Compose and Testcontainers.

The initial machine has Java 17 available. Maven was not found in PATH and the
Docker daemon was not running during setup. The Wrapper and a running Docker
engine are prerequisites for the later verification steps, not completed checks.

The application intentionally has no authentication, scheduling, or idempotency
mechanism. It is a locally runnable exercise, with no cloud deployment planned.
