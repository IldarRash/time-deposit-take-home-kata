# Requirements and acceptance criteria

This document defines scope and acceptance. Executed evidence is in [status](status.md).

## Sources and scope

The authoritative assignment is the [upstream README](https://github.com/ikigai-digital/time-deposit-take-home-kata/blob/c4ea3585e7dd0d4d902268cae83569ca512571b4/README.md).
The compatibility baseline is the Java implementation at that same commit.
Where wording and code differ, preserve the calculator's behavior because the
assignment explicitly declares it correct. Record the difference, rather than
silently changing it.

Deliver one Java application that retrieves persisted deposits and applies the
existing balance update to all persisted deposits. The API client/evaluator is
the only actor; account ownership and user authentication are outside this kata.

## Assignment requirements

| ID | Requirement | Observable acceptance |
| --- | --- | --- |
| R01 | Exactly two API operations | GET lists all deposits; one POST updates all balances. No deposit creation, withdrawal mutation, or extra business operation. |
| R02 | GET response | Every deposit has `id`, `planType`, `balance`, `days`, and `withdrawals`; empty collections are JSON arrays. |
| R03 | Persistent deposits | A database table `timeDeposits` contains integer primary key `id`, required string `planType`, required integer `days`, and required decimal `balance`. |
| R04 | Persistent withdrawals | A table `withdrawals` contains integer primary key `id`, required deposit foreign key `timeDepositId`, required decimal `amount`, and required date `date`. |
| R05 | Balance calculation | Preserve the existing Java behavior, including thresholds, unsupported strings, operation order, and rounding. |
| R06 | Shared compatibility | Keep `TimeDeposit` unchanged and retain public `void updateBalance(List<TimeDeposit>)` and the existing no-argument calculator construction. Keeping the class byte-for-byte unchanged is our conservative implementation choice. |
| R07 | Extensibility | Adding a plan's calculation rule does not require editing batch iteration or balance application. |
| R08 | Code quality | Separate calculation, application orchestration, HTTP, and persistence; introduce abstractions only for an actual boundary or varying rule. |
| R09 | AI-assisted development | Use and document a reproducible assistant workflow, project rules, prompts, tools, and actual assisted work. Do not claim future work is completed. |
| R10 | Submission | Public personal fork, readable commit history, startup/test instructions, and documented Swagger requests. Submission email is a separate author action. |
| R11 | Ambiguities | State assumptions in this document and add focused code comments where implemented behavior depends on them. |

OpenAPI/Swagger, hexagonal architecture, atomic commits, and Testcontainers are
listed as preferences upstream; this solution plans to include all four.
Custom invalid-input validation and exception handling are explicitly not required.

## Existing behavior to preserve

Let `b` be the balance before a call and `d` its stored day count.

| Exact plan string | Eligibility | Raw interest |
| --- | --- | --- |
| `basic` | `d > 30` | `b * 0.01 / 12` |
| `student` | `30 < d && d < 366` | `b * 0.03 / 12` |
| `premium` | `d > 45` | `b * 0.05 / 12` |
| Any other non-null string | Never | `0` |

The original code initializes interest to positive `0.0` and accumulates the
eligible expression with `+=`. It then applies exactly:

```java
double roundedInterest = new BigDecimal(interest)
        .setScale(2, RoundingMode.HALF_UP).doubleValue();
double newBalance = oldBalance + roundedInterest;
```

The final balance is not itself rounded. The method mutates every supplied
deposit's balance in place, does not change `days`, `id`, or `planType`, and does
not add, remove, or reorder list elements. Empty input is a no-op. An ineligible
or unknown plan must not prevent later deposits from being processed.

For unusual finite numeric inputs, a refactor must not introduce deliberate new
validation or arithmetic behavior. Nulls, NaN, infinity, and malformed HTTP input
are outside acceptance scope; do not add speculative handling.

## Assumptions and decisions

| ID | Ambiguity | Selected interpretation and reason |
| --- | --- | --- |
| A01 | README describes monthly interest, code divides rates by 12 | Preserve the formula. Treat the listed rates as annual nominal rates for one monthly update; document this wording mismatch. |
| A02 | What does a call represent? | One explicit calculation cycle on the stored `days` and current balance. It does not advance time or schedule future work. |
| A03 | Repeated update requests | Each successful POST applies another cycle. The operation is deliberately not idempotent; automatic retries can cause additional interest. No period tracking or deduplication is specified. |
| A04 | Withdrawals and calculation | Withdrawals are stored historical records returned by GET. Assume `balance` already reflects any historical withdrawals; do not subtract them again or invent withdrawal processing. |
| A05 | Withdrawal JSON shape | Return `id`, `amount`, and `date` under each deposit; the parent provides deposit identity. Use ISO `YYYY-MM-DD` dates with no time zone. |
| A06 | Endpoints and status codes | Choose `GET /time-deposits` returning 200 JSON and `POST /time-deposits/update-balances` with no body returning 204. Paths and POST status are solution decisions. |
| A07 | Swagger versus exactly two endpoints | Publish a static OpenAPI contract and an optional separate Swagger UI container on localhost:8081. Its proxy lets Execute reach the Java application on port 8080 without CORS changes. The Java application still exposes only two business operations, no documentation/Actuator handlers. Framework error handling is not an advertised API. |
| A08 | List order | Order deposits and nested withdrawals by ID for deterministic output; upstream imposes no order. |
| A09 | Empty database | GET returns `[]`; POST succeeds with 204 and changes nothing. |
| A10 | Batch transaction | One transaction loads, calculates, and persists the batch. A failure must not leave partially updated balances. This is a solution consistency decision. |
| A11 | Concurrent updates | Serialize overlapping batches with ordered row locks held until commit. Each completed POST applies one cycle without losing another request's update. |
| A12 | Money representation | Keep the legacy `Double` contract and arithmetic. Use unconstrained SQL NUMERIC. If the calculator leaves a Double balance unchanged, do not write it back: preserve the original database decimal exactly. Changed balances persist the legacy Double result through BigDecimal.valueOf, including its precision limits; arbitrary-precision interest calculation is not promised. Never force the final balance to two decimal places. |
| A13 | Initial data | Supply deterministic demo seed data through a documented database script; do not add a seed/create API. |
| A14 | Environment | Java 17, Maven 3.9.11 Wrapper, Spring Boot 3.5.16, and PostgreSQL 16.15 via Docker. The POM/BOM and container tags pin the tested versions. |

Auto-renewal, termination, additional plans, daily accrual, authentication,
pagination, background jobs, cloud deployment, and a frontend are out of scope.

## User flows and acceptance scenarios

1. The evaluator starts the documented database and application, then loads demo data.
2. GET returns persisted deposit data and only that deposit's withdrawals.
3. POST updates all deposits according to the legacy rules and commits once.
4. A subsequent GET and direct database query show the same updated balances.
5. Restarting the application preserves those balances.
6. A second POST applies a second cycle; `days` and withdrawals remain unchanged.

| ID | Scenario | Expected evidence |
| --- | --- | --- |
| AC01 | Thresholds | At balance 1200: basic day 30 stays 1200, day 31 becomes 1201; student day 30 stays 1200, days 31 and 365 become 1203, day 366 stays 1200; premium days 30, 31, and 45 stay 1200, day 46 becomes 1205. |
| AC02 | Exact matching | `unknown` and `BASIC` remain unchanged; a valid following deposit is still updated. |
| AC03 | Rounding compatibility | Basic balance 18 on day 31 becomes 18.01, not 18.02. Include additional near-half-cent and fractional-balance cases from the unchanged baseline. |
| AC04 | Batch semantics | Empty list, mixed plans, multiple eligible deposits, and repeated calls work without replacing objects or changing unrelated fields. |
| AC05 | Public compatibility | Existing construction and method calls compile; `TimeDeposit.java` remains unchanged from baseline. |
| AC06 | Retrieval | Empty DB, deposit without withdrawals, and multiple deposits with multiple withdrawals return correct JSON without duplicates or cross-association. |
| AC07 | Persistence | Database decimal columns, keys, required fields, and foreign key match R03/R04. Fractional balances survive the selected mapping. |
| AC08 | Atomicity | A controlled persistence failure in an integration test rolls back all balance writes; no custom error API is needed. |
| AC09 | Concurrent batch behavior | Two overlapping requests complete as two serialized cycles, without lost updates; verify against real PostgreSQL. |
| AC10 | Contract | Static OpenAPI contains exactly two operations, matches runtime response bodies/statuses, and includes usable requests. |
| AC11 | Reproducibility | A clean checkout can run unit tests, PostgreSQL integration tests, and the documented startup flow without local IDE state. Missing Docker must be reported, not silently treated as a passing integration suite. |
| AC12 | Development evidence | Commits separate characterization, refactoring, and feature work; verification records distinguish executed checks from planned checks. |

## Baseline evidence

On 2026-09-22 a temporary Java 17 probe compiled and ran the unchanged two
production classes. Sixteen checks passed: the ten threshold examples in AC01,
unknown and uppercase strings, the balance-18 rounding case, mixed batch,
repeated call, and empty list. This is a bounded baseline experiment, not the
repository test suite or a coverage claim. Convert these into committed tests
before refactoring.

The rounding experiment produced `interest=0.015`, but
`new BigDecimal(interest).setScale(2, HALF_UP)` returned `0.01`, while
`BigDecimal.valueOf(interest).setScale(2, HALF_UP)` returned `0.02`.
This establishes why replacing the constructor changes observable behavior.

The upstream JUnit test only asserted `1 == 1`; it provided no balance regression
protection. It has since been replaced by 57 characterization scenarios, committed
and run before refactoring. Current API, database, coverage, and runtime evidence
is tracked separately in the status document.
