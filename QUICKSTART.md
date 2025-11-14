# Quick Start Guide

## Prerequisites Check

Before running the application, ensure you have:

1. **Java 17** installed
   ```bash
   java -version
   # Should show version 17 or higher
   ```

2. **Maven** installed
   ```bash
   mvn -version
   # Should show Maven 3.8+
   ```

3. **PostgreSQL databases** running
   ```bash
   # Check payment database
   psql -h localhost -p 5446 -U payment -d payment -c "SELECT 1;"

   # Check refunds database
   psql -h localhost -p 5447 -U refunds -d refunds -c "SELECT 1;"
   ```

## Running the Application

### Option 1: Using Maven (Development)

```bash
cd db-tool-spring-boot
mvn spring-boot:run
```

### Option 2: Build and Run JAR

```bash
cd db-tool-spring-boot
mvn clean package
java -jar target/db-tool-spring-boot-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Testing the API

Once the application is running, test the endpoints:

### 1. Query by CCD Case Number

```bash
# Replace with actual CCD from your database
curl http://localhost:8080/api/cases/ccd/1111111111111111 | jq
```

### 2. Query by Payment Reference

```bash
# Replace with actual payment reference from your database
curl http://localhost:8080/api/cases/payment-reference/RC-1234-5678-9012-3456 | jq
```

### 3. Get Summary Only

```bash
curl http://localhost:8080/api/cases/ccd/1111111111111111/summary | jq
```

## Expected Response

```json
{
  "case": {
    "ccdCaseNumber": "1111111111111111",
    "serviceRequests": [
      {
        "paymentReference": "RC-1234-5678-9012-3456",
        "ccdCaseNumber": "1111111111111111",
        "orgId": "ORG123",
        "serviceName": "CMC",
        "fees": [
          {
            "code": "FEE001",
            "version": "1",
            "amount": 100.0,
            "volume": 1,
            "remissions": []
          }
        ],
        "payments": [
          {
            "reference": "PAY-001",
            "amount": 100.0,
            "currency": "GBP",
            "status": "success",
            "refunds": []
          }
        ]
      }
    ]
  },
  "summary": {
    "totalFees": 100.0,
    "totalPayments": 100.0,
    "totalRefunds": 0.0,
    "totalRemissions": 0.0,
    "serviceRequestCount": 1,
    "feeCount": 1,
    "paymentCount": 1,
    "refundCount": 0,
    "netAmount": 100.0,
    "amountDue": 0.0
  }
}
```

## Troubleshooting

### Database Connection Errors

If you see connection errors:

1. Check databases are running:
   ```bash
   docker ps | grep postgres
   ```

2. Verify connection details in `src/main/resources/application.yml`

3. Test connection manually:
   ```bash
   psql -h localhost -p 5446 -U payment -d payment
   ```

### Port Already in Use

If port 8080 is already in use, change it in `application.yml`:

```yaml
server:
  port: 8081  # Or any available port
```

### Build Errors

If Maven build fails:

1. Clean and rebuild:
   ```bash
   mvn clean install -U
   ```

2. Check Java version:
   ```bash
   java -version
   mvn -version
   ```

## Development Mode

For development with auto-reload, use Spring DevTools (already in pom.xml):

```bash
mvn spring-boot:run
```

Code changes will automatically reload the application.

## Viewing Logs

Logs are output to console. To change log level, edit `application.yml`:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.dbtool: DEBUG  # Set to INFO, WARN, or ERROR
```

## Next Steps

- See [README.md](README.md) for full documentation
- Add integration tests
- Explore the REST API with Postman or curl
- Check application health: `http://localhost:8080/actuator/health` (if actuator is added)
