package uk.gov.hmcts.reform.dbtool.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests with Testcontainers.
 * Starts two PostgreSQL containers for payments and refunds databases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIT {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> paymentsDb = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payments")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("db/payments-init.sql");

    @Container
    static PostgreSQLContainer<?> refundsDb = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("refunds")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("db/refunds-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Payment database
        registry.add("spring.datasource.payment.url", paymentsDb::getJdbcUrl);
        registry.add("spring.datasource.payment.username", paymentsDb::getUsername);
        registry.add("spring.datasource.payment.password", paymentsDb::getPassword);

        // Refunds database
        registry.add("spring.datasource.refunds.url", refundsDb::getJdbcUrl);
        registry.add("spring.datasource.refunds.username", refundsDb::getUsername);
        registry.add("spring.datasource.refunds.password", refundsDb::getPassword);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }
}