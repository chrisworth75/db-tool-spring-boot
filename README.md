# DB Tool - Spring Boot

> **Note:** This Spring Boot implementation is now the primary version of the DB Tool. The Node.js (`../db-tool`) and Python (`../db-tool-python`) versions may be behind and are not actively maintained. If you need the latest features and improvements, use this version.

A Spring Boot REST API for querying, analyzing, and cleaning up payment and refund data from two PostgreSQL databases.

## Features

- **Query case data** by CCD case number
- **Merge data** from two databases (payments and refunds)
- **Calculate case summaries** (totals, balances, counts)
- **Generate cleanup SQL** via PATCH endpoint for:
  - Deleting duplicate service requests
  - Removing orphaned fees, payments, remissions, apportionments
  - Moving entities between service requests
- **Rollback SQL generation** - INSERT statements to undo deletions
- Clean separation between domain and database models
- Dual database support with separate entity managers
- RESTful API with JSON responses
- Immutable domain model using Java records

## Architecture

This application implements a **two-model architecture**:

1. **Clean Domain Model** (`uk.gov.hmcts.reform.dbtool.domain`)
   - Immutable Java records for type safety and clarity
   - Business-focused DTOs with no database-specific fields
   - Optimized for API responses and business logic
   - Records: `Fee`, `Payment`, `Refund`, `Remission`, `Apportionment`, `ServiceRequest`, `CaseSummary`
   - Class: `Case` (has business logic methods)

2. **Database Model** (`uk.gov.hmcts.reform.dbtool.database`)
   - JPA entities suffixed with `Entity` (e.g., `FeeEntity`, `PaymentEntity`)
   - Exactly match database table structures
   - Used only for persistence layer

3. **Immutable Mapping** (`uk.gov.hmcts.reform.dbtool.mapper`)
   - `CaseMapper` builds domain objects bottom-up
   - Child collections are built first, then parent records constructed with completed lists
   - No mutation after construction

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker (for running databases and integration tests)
- PostgreSQL databases running on:
  - Payment DB: localhost:5446
  - Refunds DB: localhost:5447

## Quick Start

### 1. Start the databases

```bash
docker run -d --name payments-db -p 5446:5432 \
  -e POSTGRES_DB=payments -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  postgres:15

docker run -d --name refunds-db -p 5447:5432 \
  -e POSTGRES_DB=refunds -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  postgres:15
```

### 2. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:3500` and automatically create tables and seed data via Liquibase.

## API Endpoints

### Get Case by CCD Number

```
GET /api/cases/ccd/{ccdCaseNumber}
```

Returns the full case structure with all service requests, fees, payments, remissions, refunds, and apportionments.

### Get Case Summary Only

```
GET /api/cases/ccd/{ccdCaseNumber}/summary
```

Returns just the calculated totals and counts.

### Generate Cleanup SQL (PATCH)

```
PATCH /api/cases/ccd/{ccdCaseNumber}
```

Compares the request body with the database state and generates SQL statements for cleanup operations.

**How it works:**
- Send the case structure you want to **KEEP**
- Entities present in the request body will be kept
- Entities absent from the request will be marked for deletion
- Entities that appear under a different service request than in the database will be **moved**
- Returns SQL statements (not executed) that can be run manually

**Request Body:**
```json
{
    "ccdCaseNumber": "1000000000000005",
    "serviceRequests": [
        {
            "id": 5,
            "fees": [
                {"id": 6, "remissions": [{"hwfReference": "HWF-MOVE-001"}]},
                {"id": 7, "remissions": []}
            ],
            "payments": [
                {"id": 6, "apportionments": [{"id": 6}, {"id": 7}]}
            ]
        }
    ]
}
```

**Response:**
```json
{
    "paymentDatabaseSql": [
        "UPDATE fee SET payment_link_id = 5 WHERE id = 7;",
        "DELETE FROM payment_fee_link WHERE id = 6;"
    ],
    "refundsDatabaseSql": [],
    "paymentDatabaseRollbackSql": [
        "UPDATE fee SET payment_link_id = 6 WHERE id = 7;",
        "INSERT INTO payment_fee_link ..."
    ],
    "refundsDatabaseRollbackSql": [],
    "summary": {
        "serviceRequestsToDelete": 1,
        "feesToDelete": 0,
        "paymentsToDelete": 0,
        "remissionsToDelete": 0,
        "refundsToDelete": 0,
        "apportionmentsToDelete": 0,
        "feesToMove": 1,
        "paymentsToMove": 0,
        "remissionsToMove": 0,
        "apportionmentsToMove": 1
    }
}
```

## Seed Data (Test Cases)

The application uses Liquibase to manage database schema and seed data. Test data is loaded from:
- `src/main/resources/db/changelog/payments/seed-data.sql`
- `src/main/resources/db/changelog/refunds/seed-data.sql`

### Test Cases

| CCD Case Number | Description | Use Case |
|-----------------|-------------|----------|
| `1000000000000001` | Simple case - 1 service request, 1 fee, 1 payment, 1 apportionment | Basic queries, delete all |
| `1000000000000002` | Multiple fees and payments - 2 fees, 2 payments, 2 apportionments | Partial deletions |
| `1000000000000003` | With remission (HWF) - 1 fee with remission | Remission handling |
| `1000000000000004` | With refund - 1 payment with associated refund in separate DB | Cross-database refund handling |
| `1000000000000005` | **Duplicate service requests** - 2 payment_fee_links with fees, payments, apportionments, remissions on both | **Move operations** - move entities from one link to another |
| `1000000000000006` | Duplicate with children - 2 service requests, one to delete with all children | Cascade deletions |
| `1000000000000007` | Duplicate remissions - 1 fee with 2 remissions | Remove duplicate remissions |

### Test Case 5 - Move Scenario Detail

This is the key test case for move operations:

**Link 5 (PAY-TEST-005-A):**
- Fee 6 (Application fee, 100.00)
- Payment 6 (RC-TEST-0006, 75.00)
- Apportionment 6
- Remission HWF-MOVE-001 (25.00)

**Link 6 (PAY-TEST-005-B):**
- Fee 7 (Hearing fee, 50.00)
- Fee 8 (Court fee, 75.00)
- Payment 7 (RC-TEST-0007, 125.00)
- Apportionments 7, 8
- Remission HWF-MOVE-002 (15.00)

**Scenario:** Delete Link 6 but move its children to Link 5.

## Running Tests

### Unit Tests

```bash
mvn test
```

Runs fast unit tests with mocked repositories (`CaseDiffServiceTest`, `CaseMapperTest`, etc.)

### Integration Tests

```bash
mvn verify
```

Runs integration tests using Testcontainers (requires Docker).

### Integration Test Classes

| Test Class | Description |
|------------|-------------|
| `CasePatchApiIT` | Tests PATCH endpoint API responses and SQL generation logic |
| `SqlExecutionAndRollbackIT` | **Executes generated SQL** on real databases and verifies: <br/>- Deletions work correctly<br/>- Rollback SQL restores original state<br/>- Move operations relocate entities correctly<br/>- Rollback restores original positions |

### What `SqlExecutionAndRollbackIT` Tests

1. **`shouldDeleteAndRollbackEntities`** - Full cycle: delete everything → verify deleted → rollback → verify restored
2. **`shouldDeleteAndRollbackRefunds`** - Tests separate refunds database handling
3. **`shouldMoveAndRollbackEntities`** - Move fees between service requests → rollback → verify original positions
4. **`shouldPartiallyDeleteAndRollback`** - Delete one of multiple fees → rollback
5. **`shouldProduceNoEffectWhenNoChanges`** - Keeping everything produces empty SQL

## Project Structure

```
src/main/java/uk/gov/hmcts/reform/dbtool/
├── config/              # Database configuration (dual datasources)
│   ├── PaymentDataSourceConfig.java
│   └── RefundDataSourceConfig.java
├── controller/          # REST API controllers
│   └── CaseController.java
├── database/            # JPA entities (database model)
│   ├── PaymentFeeLinkEntity.java
│   ├── FeeEntity.java
│   ├── PaymentEntity.java
│   ├── RefundEntity.java
│   ├── RemissionEntity.java
│   └── ApportionmentEntity.java
├── domain/              # Immutable domain records
│   ├── Case.java
│   ├── ServiceRequest.java
│   ├── Fee.java, Payment.java, Refund.java, Remission.java, Apportionment.java
│   ├── CaseSummary.java
│   ├── CasePatchRequest.java      # PATCH request DTO
│   └── SqlGenerationResult.java   # PATCH response with SQL statements
├── mapper/              # Maps between database and domain models
│   └── CaseMapper.java
├── repository/          # JPA repositories
├── service/
│   ├── CaseQueryService.java      # GET endpoint logic
│   └── CaseDiffService.java       # PATCH endpoint SQL generation
└── DbToolApplication.java

src/main/resources/
├── application.yml
└── db/changelog/
    ├── payments/
    │   ├── db.changelog-master.yaml
    │   └── seed-data.sql
    └── refunds/
        ├── db.changelog-master.yaml
        └── seed-data.sql

src/test/java/uk/gov/hmcts/reform/dbtool/
├── e2e/                           # Integration tests (require Docker)
│   ├── BaseIT.java                # Testcontainers setup
│   ├── CasePatchApiIT.java        # PATCH API tests
│   └── SqlExecutionAndRollbackIT.java  # SQL execution verification
├── service/
│   ├── CaseQueryServiceTest.java
│   └── CaseDiffServiceTest.java   # Unit tests for SQL generation
└── ...
```

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Database connection details
- Logging levels
- Server port (default: 3500)

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - Database access
- **PostgreSQL 15** - Database
- **Hibernate 6** - ORM
- **Liquibase** - Database migrations and seed data
- **Lombok** - Reduces boilerplate code
- **MapStruct** - Object mapping
- **Java Records** - Immutable domain model
- **JUnit 5** - Testing
- **Testcontainers** - Integration testing with real databases
- **RestAssured** - API testing

## API Collections

### Postman

Import `postman/db-tool-api.postman_collection.json` for ready-to-use API requests.

### Bruno

The `bruno/` directory contains Bruno API collection files (gitignored).

## License

Copyright (c) 2024 HMCTS