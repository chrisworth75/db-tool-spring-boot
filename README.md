# DB Tool - Spring Boot

A Spring Boot REST API for querying and merging payment and refund data from two PostgreSQL databases.

## Architecture

This application implements a **two-model architecture**:

1. **Clean Domain Model** (`uk.gov.hmcts.reform.dbtool.domain`)
   - Business-focused DTOs with no database-specific fields
   - Optimized for API responses and business logic
   - Classes: `Case`, `ServiceRequest`, `Fee`, `Payment`, `Refund`, `Remission`

2. **Database Model** (`uk.gov.hmcts.reform.dbtool.database`)
   - JPA entities that exactly match database tables
   - Include validation annotations (@NotNull, @Size, @Positive, etc.)
   - Used only for persistence layer

## Features

- ✅ Query case data by CCD case number
- ✅ Query case data by payment reference
- ✅ Merge data from two databases (payments and refunds)
- ✅ Calculate case summaries (totals, balances, counts)
- ✅ Clean separation between domain and database models
- ✅ Dual database support with separate transaction managers
- ✅ RESTful API with JSON responses

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL databases running on:
  - Payment DB: localhost:5446
  - Refunds DB: localhost:5447

## Database Setup

The application expects two PostgreSQL databases:

```bash
# Payment Database
Host: localhost
Port: 5446
Database: payment
Username: payment
Password: payment

# Refunds Database
Host: localhost
Port: 5447
Database: refunds
Username: refunds
Password: refunds
```

## Building

```bash
mvn clean install
```

## Running

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Get Case by CCD Number

```bash
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
        "paymentReference": "RC-1234-5678-9012-3456",
        "fees": [...],
        "payments": [...]
      }
    ]
  },
  "summary": {
    "totalFees": 200.00,
    "totalPayments": 150.00,
    "totalRefunds": 0.00,
    "totalRemissions": 50.00,
    "amountDue": 0.00,
    "netAmount": 200.00,
    "serviceRequestCount": 1,
    "feeCount": 2,
    "paymentCount": 1,
    "refundCount": 0
  }
}
```

### Get Case by Payment Reference

```bash
GET /api/cases/payment-reference/{paymentReference}
```

Example:
```bash
curl http://localhost:8080/api/cases/payment-reference/RC-1234-5678-9012-3456
```

### Get Case Summary Only

```bash
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
│   ├── PaymentFeeLink.java
│   ├── Fee.java
│   ├── Payment.java
│   ├── Refund.java
│   ├── Remission.java
│   └── Apportionment.java
├── domain/              # Clean domain DTOs
│   ├── Case.java
│   ├── ServiceRequest.java
│   ├── Fee.java
│   ├── Payment.java
│   ├── Refund.java
│   ├── Remission.java
│   ├── CaseSummary.java
│   └── ServiceRequestSummary.java
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
- **Hibernate** - ORM
- **Lombok** - Reduces boilerplate code
- **Jakarta Validation** - Bean validation
- **SLF4J + Logback** - Logging

## Future Enhancements

- [ ] Add integration tests
- [ ] Add POST endpoints for creating cases
- [ ] Add bidirectional mapping (domain → database)
- [ ] Add comprehensive validation before persistence
- [ ] Add Swagger/OpenAPI documentation
- [ ] Add health check endpoints
- [ ] Add metrics and monitoring

## Comparison with Node.js Version

This Spring Boot version mirrors the Node.js implementation in `../db-tool`:

| Feature | Node.js | Spring Boot |
|---------|---------|-------------|
| Clean Domain Model | ✅ | ✅ |
| Database Model | ✅ | ✅ |
| Validation | Custom validators | Jakarta Validation |
| Database Access | pg library | Spring Data JPA |
| API | CLI tool | REST API |
| Mapping | Manual functions | Service layer |
| Tests | Jest | (TBD) |

## License

Copyright (c) 2024 HMCTS
