# DB Tool - Spring Boot

> **Note:** This Spring Boot implementation is now the primary version of the DB Tool. The Node.js (`../db-tool`) and Python (`../db-tool-python`) versions may be behind and are not actively maintained. If you need the latest features and improvements, use this version.

A Spring Boot REST API for querying and merging payment and refund data from two PostgreSQL databases.

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

## Features

- Query case data by CCD case number
- Merge data from two databases (payments and refunds)
- Calculate case summaries (totals, balances, counts)
- Clean separation between domain and database models
- Dual database support with separate entity managers
- RESTful API with JSON responses
- Immutable domain model using Java records

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL databases running on:
  - Payment DB: localhost:5446
  - Refunds DB: localhost:5447

## Database Setup

The application expects two PostgreSQL databases:

```
# Payment Database
Host: localhost
Port: 5446
Database: payments
Username: postgres
Password: postgres

# Refunds Database
Host: localhost
Port: 5447
Database: refunds
Username: postgres
Password: postgres
```

## Building

```bash
mvn clean package
```

## Running

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/db-tool-spring-boot-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Get Case by CCD Number

```
GET /api/cases/ccd/{ccdCaseNumber}
```

Example:
```bash
curl http://localhost:8080/api/cases/ccd/1111111111111111
```

Response:
```json
{
  "case": {
    "ccdCaseNumber": "1111111111111111",
    "serviceRequests": [
      {
        "id": 1,
        "paymentReference": "PAY-1111",
        "fees": [...],
        "payments": [...]
      }
    ]
  },
  "summary": {
    "totalFees": 630,
    "totalPayments": 630,
    "totalRefunds": 0,
    "totalRemissions": 0,
    "amountDue": 0,
    "netAmount": 630,
    "serviceRequestCount": 1,
    "feeCount": 3,
    "paymentCount": 3,
    "refundCount": 0,
    "remissionCount": 0
  }
}
```

### Get Case Summary Only

```
GET /api/cases/ccd/{ccdCaseNumber}/summary
```

Example:
```bash
curl http://localhost:8080/api/cases/ccd/1111111111111111/summary
```

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
│   ├── Case.java           # Class with getSummary() method
│   ├── ServiceRequest.java # Record
│   ├── Fee.java            # Record
│   ├── Payment.java        # Record
│   ├── Refund.java         # Record
│   ├── Remission.java      # Record
│   ├── Apportionment.java  # Record
│   └── CaseSummary.java    # Record with @Builder
├── mapper/              # Maps between database and domain models
│   └── CaseMapper.java
├── repository/          # JPA repositories
│   ├── PaymentFeeLinkRepository.java
│   ├── FeeRepository.java
│   ├── PaymentRepository.java
│   ├── RefundRepository.java
│   ├── RemissionRepository.java
│   └── ApportionmentRepository.java
├── service/             # Business logic
│   └── CaseQueryService.java
└── DbToolApplication.java

src/test/java/uk/gov/hmcts/reform/dbtool/
├── domain/
│   ├── CaseTest.java
│   └── DomainModelTest.java
├── mapper/
│   └── CaseMapperTest.java
├── service/
│   └── CaseQueryServiceTest.java
└── DbToolApplicationTest.java
```

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Database connection details
- Logging levels
- Server port

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **Hibernate 6** - ORM
- **Lombok** - Reduces boilerplate code (@Builder for CaseSummary)
- **Java Records** - Immutable domain model
- **JUnit 5** - Testing

## Running Tests

```bash
mvn test
```

## License

Copyright (c) 2024 HMCTS