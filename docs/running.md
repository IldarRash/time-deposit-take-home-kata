# Run and verify the Java solution

## Prerequisites

- JDK 17; `java -version` must report Java 17 and `JAVA_HOME` must point to its JDK.
- Docker with Linux containers and Docker Compose v2; `docker info` must succeed.
- Network access for the first Maven dependency download and PostgreSQL/Swagger image pulls.

Maven 3.9.11 is downloaded by the checked-in Wrapper. No IDE or globally installed
Maven is required. The database credentials below are local demo defaults.

## Start the application

From the repository root:

```sh
docker compose up -d --wait db
cd java
./mvnw spring-boot:run
```

On Windows replace `./mvnw` with `.\mvnw.cmd`. Keep this terminal running.
Flyway creates the schema on startup; the database initially contains no deposits.
The application listens on `http://localhost:8080`.

In a second terminal at the repository root, optionally load demo data:

```sh
docker compose exec -T db psql -U deposits -d time_deposits -v ON_ERROR_STOP=1 < java/demo/seed.sql
```

PowerShell equivalent:

```powershell
Get-Content java/demo/seed.sql | docker compose exec -T db psql -U deposits -d time_deposits -v ON_ERROR_STOP=1
```

The seed script can be rerun: it preserves existing balances and records.
The Compose volume retains data when the application or database is restarted.

## Swagger contract and requests

With the Java application running on port 8080, start the optional documentation
container from the repository root:

```sh
docker compose --profile docs up -d swagger
```

Open **http://localhost:8081**. The pinned Swagger UI 5.33.0 loads the checked-in
[java/openapi.yaml](../java/openapi.yaml) automatically. Expand **GET /time-deposits**,
click **Try it out**, then **Execute**: the server response is 200 and a JSON array.
Expand **POST /time-deposits/update-balances**, click **Try it out** and **Execute**:
expect 204 with no body. Execute GET again to see the persisted updated balances.

The contract uses the relative server `/`. The documentation container proxies
only those two API paths to the Java application on the host at port 8080, so
browser requests stay on the UI's origin and need no CORS exception. Swagger UI
is a separate local tool, not an extra application endpoint. Neither Node nor an
IDE is needed to use it. The hosted Swagger Editor is not the execution workflow.
See the upstream [Swagger UI installation](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/)
and [configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/) documentation.

Equivalent terminal requests are:

```sh
curl -i http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/update-balances
curl -i http://localhost:8080/time-deposits
```

On Windows use `curl.exe` to avoid a PowerShell alias. GET returns 200 and an array;
POST takes no body and returns 204 with no response body. There is no application
Swagger UI route, creation API, withdrawal API, or Actuator endpoint.

For a freshly seeded database, the first POST yields these balances:

| ID | Plan / days | Before | After |
| --- | --- | --- | --- |
| 1 | basic / 31 | 1200.00 | 1201.00 |
| 2 | student / 365 | 1200.00 | 1203.00 |
| 3 | premium / 46 | 1200.00 | 1205.00 |
| 4 | student / 366 | 1200.00 | 1200.00 |
| 5 | premium / 45 | 1200.00 | 1200.00 |
| 6 | unknown / 60 | 1200.00 | 1200.00 |

Each POST applies another cycle. It is not idempotent, does not advance days, and
does not subtract historical withdrawals. Decimal JSON formatting may omit zeros.
The preserved rounding rules and other assumptions are in [requirements](requirements.md).

Stop the application with Ctrl+C and the containers with `docker compose --profile docs down`.
That command retains the database volume and its data.

## Tests and packaged application

From `java/`:

```sh
./mvnw test       # Unit/contract tests; no database required
./mvnw verify     # Also runs PostgreSQL Testcontainers integration tests
java -jar target/time-deposit-kata-1.0-SNAPSHOT.jar
```

The last command runs the packaged application against the same Compose database;
stop any existing application process using port 8080 first. Failsafe integration
tests use their own temporary PostgreSQL container and random database port.
Missing Docker makes full verification fail; integration tests are not silently skipped.

Reports: `java/target/surefire-reports`, `java/target/failsafe-reports`, and
`java/target/site/jacoco/index.html`. Verification requires 100% line and branch
coverage for the calculator and plan rules, alongside behavior assertions.

GitHub Actions runs full verification on Java 17, then
[scripts/smoke-test.sh](../scripts/smoke-test.sh). This Linux smoke check uses a
separate Compose project and database port 55432, starts the packaged JAR, loads
demo data, performs two accrual cycles, and verifies state after an application
restart. It then opens the real Swagger UI in Chromium and clicks Execute for
GET -> POST -> GET, asserting a third persisted cycle and unchanged withdrawals/days.
The browser screenshot is retained with the CI test artifacts.

To run that same smoke check locally on Linux, first build with `./mvnw verify`,
then install its CI-only browser tooling (Node 22) and run from the repository root:

```sh
npm ci --prefix scripts/browser-check
cd scripts/browser-check
npx playwright install --with-deps chromium
cd ../..
bash scripts/smoke-test.sh
```

Smoke requires Bash, Python 3, curl, Docker, Node 22 and free ports 8080/8081/55432.
This Node dependency is only for automated browser verification; the application
and manual Swagger flow remain Java/Docker only. The setup follows the
[Playwright CI guidance](https://playwright.dev/docs/ci).

Configuration: `DB_URL`, `DB_USER`, and `DB_PASSWORD` override the application
database connection. `DB_PORT` changes Compose's published database port; if
changed, set the corresponding JDBC port in `DB_URL` as well.
