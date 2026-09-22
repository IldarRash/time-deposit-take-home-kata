# Architecture and compatibility decisions

Implemented design for [the requirements](requirements.md).
See [status](status.md) for the exact verification evidence and remaining handoff.

## Language and stack

Use **Java 17** in the existing `java/` Maven project. It matches the supplied
sources and minimum Java version, keeps the exercise focused on Java refactoring,
and avoids translating the compatibility baseline to another language.
Preserve the other language examples without modifying them.

Stack: Spring Boot 3.5.16 for startup, HTTP and transaction wiring; Spring JDBC
for explicit SQL and mapping; PostgreSQL for persistence; Flyway for schema
migrations; JUnit 5 and AssertJ for tests; JaCoCo for domain coverage;
Testcontainers 1.21.4 for real PostgreSQL 16.15 integration tests; Maven 3.9.11
via its Wrapper. Dependency versions are pinned by the Maven POM and Spring Boot
BOM; the same PostgreSQL image version is used in Compose and tests.

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
- Use-case orchestration and query records live in `application`, the controller
  in `adapter.in.web`, and JDBC mapping in `adapter.out.persistence`.
  `TimeDepositApplication` contains the one calculator bean; a separate
  configuration hierarchy is unnecessary.
- Define a persistence port around loading deposits (including a locking batch
  read), reading withdrawals, and saving balances. Keep SQL and framework
  annotations out of the calculator and plan rules.
- Spring's `@Transactional` at the application service boundary owns the read,
  calculation, and writes. This small framework dependency keeps orchestration
  explicit without a custom transaction abstraction. The calculator and rules
  remain free of framework annotations.

## API contract

| Operation | Input | Successful response |
| --- | --- | --- |
| `GET /time-deposits` | None | 200, array of deposits with nested withdrawals |
| `POST /time-deposits/update-balances` | No body | 204, no body |

Example GET response:

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
The static `java/openapi.yaml` describes both operations. An optional standalone
Swagger UI container proxies these exact paths to the application, allowing browser
Execute on the same origin without extra Java documentation endpoints or CORS rules.

## Database and transactions

Use exact logical assignment names; in PostgreSQL migrations quote camel-case
identifiers such as `"timeDeposits"`, `"planType"`, and `"timeDepositId"`.
Use integer primary keys, required columns, a withdrawal foreign key, and an
index on the foreign key. Use `DATE` for withdrawal dates.

Use unconstrained PostgreSQL `NUMERIC` for balance and amount, avoiding an
invented two-decimal storage rule. Map database decimals to legacy double values
only at the calculation boundary. The service snapshots those Double values and
saves only changed balances, leaving original database decimals untouched for
unknown/ineligible plans and for interest rounded to zero. This also preserves
digits that the legacy Double cannot represent when no update is needed.
Changed finite results are mapped back through `BigDecimal.valueOf`. That
conversion is separate from the legacy `new BigDecimal(interest)` rounding and
must not replace it.

Testcontainers round-trip checks cover half-cent cases, fractional balances, and
repeated updates. The reloaded finite double matches the fixed baseline result,
and SQL adds no extra two-decimal rounding. Changed balances retain the Double
result's precision limits: exact arbitrary-precision interest arithmetic is
outside the legacy contract. No-op updates retain the original NUMERIC value,
including high-precision fractions and integers beyond exact Double precision.

For updates, select deposits ordered by ID with `FOR UPDATE`, calculate, and
persist balances in one transaction. PostgreSQL's default READ COMMITTED is the
isolation level; a test observes lock contention between overlapping transactions.
Withdrawals are unchanged. No application endpoint inserts or deletes deposits.
Concurrent out-of-band database edits are outside the demonstrated guarantee.

For GET, load deposits and withdrawals in one ordered LEFT JOIN mapped by deposit
ID, avoiding one query per deposit and
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

The Wrapper and unit tests run locally on Java 17. The local sandbox cannot
access Docker's named pipe, so PostgreSQL integration tests run in GitHub Actions.
See the status log for executed results; no local database test pass is claimed.

The application intentionally has no authentication, scheduling, or idempotency
mechanism. It is a locally runnable exercise, with no cloud deployment planned.
