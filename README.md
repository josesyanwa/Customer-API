# Banking REST API

A Java 17 and Spring Boot REST API for customer deposits, account-to-account transfers, and balance lookup. Spring Data JPA persists accounts and transaction audit records in PostgreSQL. Monetary values use `BigDecimal`; deposits and transfers are transactional, and account rows are locked during balance changes to prevent concurrent overdrafts.

## Requirements

- Java 17+
- Maven 3.9+
- Docker with Docker Compose (or a PostgreSQL 16 database)

## Run locally

Start PostgreSQL:

```bash
docker compose up -d
```

The defaults in `application.yml` match `compose.yaml`. Override them with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for another PostgreSQL instance.

Build and test:

```bash
mvn clean install
```

Start the application:

```bash
mvn spring-boot:run
```

Hibernate creates/updates the tables on startup by default. Set `JPA_DDL_AUTO=validate` in environments where schema changes are managed separately. Three sample USD accounts are inserted only when their account numbers do not already exist:

| Account number | Customer | Starting balance |
|---|---|---:|
| `1000000001` | Alex Morgan | 1250.00 USD |
| `1000000002` | Jordan Lee | 800.00 USD |
| `1000000003` | Taylor Kim | 2500.00 USD |

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
