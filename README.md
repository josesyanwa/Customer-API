# Banking REST API

A Java 17 and Spring Boot REST API for customer deposits, account-to-account transfers, and balance lookup. Spring Data JPA persists accounts and transaction audit records in PostgreSQL. Monetary values use `BigDecimal`; deposits and transfers are transactional, and account rows are locked during balance changes to prevent concurrent overdrafts.

## Requirements

- Java 17+
- Docker with the Docker Compose plugin (or a PostgreSQL 16 database)

## Run locally

Start PostgreSQL from the project root:

```bash
docker compose up -d
```

If your Docker installation uses the legacy standalone Compose command instead, use `docker-compose up -d`.

Compose creates the `banking` database with the local-development credentials `banking` / `banking`. These match the defaults in `src/main/resources/application.yml`:

| Setting | Default |
|---|---|
| JDBC URL | `jdbc:postgresql://localhost:5432/banking` |
| Username | `banking` |
| Password | `banking` |

Wait for PostgreSQL to become healthy before starting the application:

```bash
docker compose ps
```

With the legacy standalone command, use `docker-compose ps`.

The Maven Wrapper downloads Maven 3.9.9 on first use, so a separate Maven installation is not required.

Build and test:

```bash
./mvnw clean install
```

The automated integration test uses an in-memory H2 database and exercises account creation, deposits, transfers, and persisted balance lookups over the HTTP routes.

Start the application:

```bash
./mvnw spring-boot:run
```

Hibernate creates/updates the `accounts` and `transactions` tables on startup by default. Set `JPA_DDL_AUTO=validate` in environments where schema changes are managed separately. The application starts without creating sample accounts; create accounts through the API. Account data already present in your database is not deleted.

## API

All request bodies are JSON. Amounts must be positive, have at most 13 integer digits and 2 decimal places. Unknown JSON properties are rejected. Transaction reference IDs are unique; reusing one returns `409 Conflict`. Error responses use the common envelope:

```json
{
  "status": 400,
  "timestamp": "2025-01-01T12:00:00Z",
  "message": "Request validation failed",
  "path": "/api/transfers",
  "fieldErrors": {
    "amount": "must be greater than 0"
  }
}
```

### Record a deposit

`POST /api/transactions` accepts `DEPOSIT` transactions only. Deposits are credited to the specified account and audited atomically.

```bash
curl -i http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "referenceId": "deposit-2025-0001",
    "accountNumber": "1000000001",
    "amount": 125.50,
    "type": "DEPOSIT",
    "description": "Initial deposit"
  }'
```

### Create an account

`POST /api/accounts` creates an account with a zero balance. The account number must be unique, and the currency must be a 3-letter uppercase code.

```bash
curl -i http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "accountNumber": "1000000001",
    "customerName": "Alex Morgan",
    "currency": "USD"
  }'
```

The response includes the new account details and its initial balance. Duplicate account numbers return `409 Conflict`.

### Transfer funds

`POST /api/transfers` atomically debits and credits accounts in the same currency. Transfers to the same account and transfers with insufficient funds are rejected.

```bash
curl -i http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "referenceId": "transfer-2025-0001",
    "sourceAccountNumber": "1000000001",
    "destinationAccountNumber": "1000000002",
    "amount": 50.00
  }'
```

### Retrieve a balance

`GET /api/accounts/{accountNumber}/balance`

```bash
curl -i http://localhost:8080/api/accounts/1000000001/balance
```

The response contains the account number, account holder, a two-decimal balance string, currency, and the last-updated timestamp.

## Configuration

The default PostgreSQL configuration in `src/main/resources/application.yml` is:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/banking}
    username: ${DB_USERNAME:banking}
    password: ${DB_PASSWORD:banking}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}
```

Override database credentials through environment variables; do not commit production secrets. The sample Compose credentials are for local development only.
