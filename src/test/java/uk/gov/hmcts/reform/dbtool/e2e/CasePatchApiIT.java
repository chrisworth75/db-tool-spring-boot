package uk.gov.hmcts.reform.dbtool.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the PATCH /api/cases/ccd/{ccdCaseNumber} endpoint.
 * Tests the SQL generation for case cleanup operations.
 */
class CasePatchApiIT extends BaseIT {

    @Nested
    @DisplayName("PATCH /api/cases/ccd/{ccdCaseNumber} - Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("should return 404 for non-existent case")
        void shouldReturn404ForNonExistentCase() {
            String requestBody = """
                {
                    "ccdCaseNumber": "9999999999999999",
                    "serviceRequests": []
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/9999999999999999")
            .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("should return 400 when path and body CCD numbers don't match")
        void shouldReturn400WhenCcdNumbersMismatch() {
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000001",
                    "serviceRequests": []
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000002")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("should return empty SQL when keeping all entities")
        void shouldReturnEmptySqlWhenKeepingAll() {
            // Test Case 1: Keep the single service request with its fee and payment
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000001",
                    "serviceRequests": [
                        {
                            "id": 1,
                            "fees": [{"id": 1}],
                            "payments": [{"id": 1}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000001")
            .then()
                .statusCode(200)
                .body("paymentDatabaseSql", hasSize(0))
                .body("refundsDatabaseSql", hasSize(0))
                .body("summary.serviceRequestsToDelete", equalTo(0))
                .body("summary.feesToDelete", equalTo(0))
                .body("summary.paymentsToDelete", equalTo(0));
        }

        @Test
        @DisplayName("should return proper response structure")
        void shouldReturnProperResponseStructure() {
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000001",
                    "serviceRequests": [{"id": 1, "fees": [{"id": 1}], "payments": [{"id": 1}]}]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000001")
            .then()
                .statusCode(200)
                .body("paymentDatabaseSql", notNullValue())
                .body("refundsDatabaseSql", notNullValue())
                .body("summary", notNullValue())
                .body("summary.serviceRequestsToDelete", notNullValue())
                .body("summary.feesToDelete", notNullValue())
                .body("summary.paymentsToDelete", notNullValue())
                .body("summary.remissionsToDelete", notNullValue())
                .body("summary.refundsToDelete", notNullValue())
                .body("summary.apportionmentsToDelete", notNullValue());
        }
    }

    @Nested
    @DisplayName("PATCH - Delete Duplicate Service Requests")
    class DeleteDuplicateServiceRequests {

        @Test
        @DisplayName("should generate SQL to delete empty duplicate service request (Test Case 5)")
        void shouldDeleteEmptyDuplicateServiceRequest() {
            // Test Case 5: Two service requests, second one is empty duplicate
            // Keep service request 5 (with data), delete service request 6 (empty)
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000005",
                    "serviceRequests": [
                        {
                            "id": 5,
                            "fees": [{"id": 6}],
                            "payments": [{"id": 6}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000005")
            .then()
                .statusCode(200)
                .body("summary.serviceRequestsToDelete", equalTo(1))
                .body("summary.feesToDelete", equalTo(0))
                .body("summary.paymentsToDelete", equalTo(0))
                .body("paymentDatabaseSql", hasSize(1))
                .body("paymentDatabaseSql[0]", containsString("DELETE FROM payment_fee_link WHERE id = 6"));
        }

        @Test
        @DisplayName("should generate SQL to delete duplicate service request with its children (Test Case 6)")
        void shouldDeleteDuplicateServiceRequestWithChildren() {
            // Test Case 6: Two service requests, both have data
            // Keep service request 7 (first one), delete service request 8 with all its children
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000006",
                    "serviceRequests": [
                        {
                            "id": 7,
                            "fees": [{"id": 7}],
                            "payments": [{"id": 7}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000006")
            .then()
                .statusCode(200)
                .body("summary.serviceRequestsToDelete", equalTo(1))
                .body("summary.feesToDelete", equalTo(1))
                .body("summary.paymentsToDelete", equalTo(1))
                .body("summary.apportionmentsToDelete", equalTo(1))
                .body("summary.refundsToDelete", equalTo(1))
                // Verify SQL contains deletions in correct order (children before parent)
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM fee_pay_apportion WHERE id = 8")))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM fee WHERE id = 8")))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM payment WHERE id = 8")))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM payment_fee_link WHERE id = 8")))
                // Refund should be in separate database SQL list
                .body("refundsDatabaseSql", hasSize(1))
                .body("refundsDatabaseSql[0]", containsString("DELETE FROM refunds WHERE id = 2"));
        }

        @Test
        @DisplayName("should identify service request by paymentReference instead of id")
        void shouldIdentifyByPaymentReference() {
            // Test Case 5: Use payment reference instead of ID
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000005",
                    "serviceRequests": [
                        {
                            "paymentReference": "PAY-TEST-005",
                            "fees": [{"id": 6}],
                            "payments": [{"id": 6}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000005")
            .then()
                .statusCode(200)
                .body("summary.serviceRequestsToDelete", equalTo(1))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM payment_fee_link WHERE id = 6")));
        }
    }

    @Nested
    @DisplayName("PATCH - Delete Fees and Payments")
    class DeleteFeesAndPayments {

        @Test
        @DisplayName("should generate SQL to delete one of multiple fees (Test Case 2)")
        void shouldDeleteOneOfMultipleFees() {
            // Test Case 2: Has 2 fees (id 2 and 3), delete fee 3
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000002",
                    "serviceRequests": [
                        {
                            "id": 2,
                            "fees": [{"id": 2}],
                            "payments": [{"id": 2}, {"id": 3}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000002")
            .then()
                .statusCode(200)
                .body("summary.feesToDelete", equalTo(1))
                .body("summary.paymentsToDelete", equalTo(0))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM fee WHERE id = 3")));
        }

        @Test
        @DisplayName("should generate SQL to delete one of multiple payments (Test Case 2)")
        void shouldDeleteOneOfMultiplePayments() {
            // Test Case 2: Has 2 payments (id 2 and 3), delete payment 3
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000002",
                    "serviceRequests": [
                        {
                            "id": 2,
                            "fees": [{"id": 2}, {"id": 3}],
                            "payments": [{"id": 2}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000002")
            .then()
                .statusCode(200)
                .body("summary.paymentsToDelete", equalTo(1))
                .body("summary.apportionmentsToDelete", equalTo(1))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM payment WHERE id = 3")))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM fee_pay_apportion WHERE id = 3")));
        }

        @Test
        @DisplayName("should identify payment by reference instead of id")
        void shouldIdentifyPaymentByReference() {
            // Test Case 2: Use payment reference to identify payment to keep
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000002",
                    "serviceRequests": [
                        {
                            "id": 2,
                            "fees": [{"id": 2}, {"id": 3}],
                            "payments": [{"reference": "RC-TEST-0002"}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000002")
            .then()
                .statusCode(200)
                .body("summary.paymentsToDelete", equalTo(1))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM payment WHERE id = 3")));
        }
    }

    @Nested
    @DisplayName("PATCH - Delete Remissions")
    class DeleteRemissions {

        @Test
        @DisplayName("should generate SQL to delete duplicate remission (Test Case 7)")
        void shouldDeleteDuplicateRemission() {
            // Test Case 7: Has 2 remissions on fee 9 (HWF-AAA-001 and HWF-AAA-002)
            // Keep only HWF-AAA-001
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000007",
                    "serviceRequests": [
                        {
                            "id": 9,
                            "fees": [
                                {
                                    "id": 9,
                                    "remissions": [{"hwfReference": "HWF-AAA-001"}]
                                }
                            ],
                            "payments": [{"id": 9}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000007")
            .then()
                .statusCode(200)
                .body("summary.remissionsToDelete", equalTo(1))
                .body("paymentDatabaseSql", hasItem(containsString("DELETE FROM remission WHERE id = 3")));
        }

        @Test
        @DisplayName("should keep remission when included in patch (Test Case 3)")
        void shouldKeepRemissionWhenIncluded() {
            // Test Case 3: Has remission HWF-123-456, keep it
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000003",
                    "serviceRequests": [
                        {
                            "id": 3,
                            "fees": [
                                {
                                    "id": 4,
                                    "remissions": [{"hwfReference": "HWF-123-456"}]
                                }
                            ],
                            "payments": [{"id": 4}]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000003")
            .then()
                .statusCode(200)
                .body("summary.remissionsToDelete", equalTo(0));
        }
    }

    @Nested
    @DisplayName("PATCH - Delete Refunds (Separate Database)")
    class DeleteRefunds {

        @Test
        @DisplayName("should generate SQL to delete refund from separate database (Test Case 4)")
        void shouldDeleteRefundFromSeparateDatabase() {
            // Test Case 4: Has refund RF-TEST-0001 on payment RC-TEST-0005
            // Keep payment but delete refund
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000004",
                    "serviceRequests": [
                        {
                            "id": 4,
                            "fees": [{"id": 5}],
                            "payments": [
                                {
                                    "id": 5,
                                    "refunds": []
                                }
                            ]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000004")
            .then()
                .statusCode(200)
                .body("summary.refundsToDelete", equalTo(1))
                .body("refundsDatabaseSql", hasSize(1))
                .body("refundsDatabaseSql[0]", containsString("DELETE FROM refunds WHERE id = 1"))
                // Payment DB SQL should NOT contain refund deletion
                .body("paymentDatabaseSql", not(hasItem(containsString("refunds"))));
        }

        @Test
        @DisplayName("should keep refund when included in patch (Test Case 4)")
        void shouldKeepRefundWhenIncluded() {
            // Test Case 4: Keep the refund
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000004",
                    "serviceRequests": [
                        {
                            "id": 4,
                            "fees": [{"id": 5}],
                            "payments": [
                                {
                                    "id": 5,
                                    "refunds": [{"reference": "RF-TEST-0001"}]
                                }
                            ]
                        }
                    ]
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000004")
            .then()
                .statusCode(200)
                .body("summary.refundsToDelete", equalTo(0))
                .body("refundsDatabaseSql", hasSize(0));
        }
    }

    @Nested
    @DisplayName("PATCH - Delete All Entities")
    class DeleteAllEntities {

        @Test
        @DisplayName("should generate SQL to delete everything when empty patch")
        void shouldDeleteEverythingWhenEmptyPatch() {
            // Test Case 1: Delete everything
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000001",
                    "serviceRequests": []
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000001")
            .then()
                .statusCode(200)
                .body("summary.serviceRequestsToDelete", equalTo(1))
                .body("summary.feesToDelete", equalTo(1))
                .body("summary.paymentsToDelete", equalTo(1))
                .body("summary.apportionmentsToDelete", equalTo(1))
                // Verify deletion order in SQL (children before parents)
                .body("paymentDatabaseSql", hasItem(containsString("fee_pay_apportion")))
                .body("paymentDatabaseSql", hasItem(containsString("fee")))
                .body("paymentDatabaseSql", hasItem(containsString("payment")))
                .body("paymentDatabaseSql", hasItem(containsString("payment_fee_link")));
        }
    }

    @Nested
    @DisplayName("PATCH - SQL Deletion Order")
    class SqlDeletionOrder {

        @Test
        @DisplayName("should delete children before parents in correct order")
        void shouldDeleteInCorrectOrder() {
            // Test Case 6: Delete service request 8 with all children
            String requestBody = """
                {
                    "ccdCaseNumber": "1000000000000006",
                    "serviceRequests": [
                        {
                            "id": 7,
                            "fees": [{"id": 7}],
                            "payments": [{"id": 7}]
                        }
                    ]
                }
                """;

            String[] response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .patch("/cases/ccd/1000000000000006")
            .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getObject("paymentDatabaseSql", String[].class);

            // Find indices of each delete type
            int apportionmentIdx = -1;
            int feeIdx = -1;
            int paymentIdx = -1;
            int linkIdx = -1;

            for (int i = 0; i < response.length; i++) {
                if (response[i].contains("fee_pay_apportion")) apportionmentIdx = i;
                if (response[i].contains("DELETE FROM fee WHERE")) feeIdx = i;
                if (response[i].contains("DELETE FROM payment WHERE")) paymentIdx = i;
                if (response[i].contains("payment_fee_link")) linkIdx = i;
            }

            // Verify order: apportionments -> fees/payments -> payment_fee_link
            org.junit.jupiter.api.Assertions.assertTrue(
                apportionmentIdx < feeIdx,
                "Apportionments should be deleted before fees"
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                apportionmentIdx < paymentIdx,
                "Apportionments should be deleted before payments"
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                feeIdx < linkIdx,
                "Fees should be deleted before payment_fee_link"
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                paymentIdx < linkIdx,
                "Payments should be deleted before payment_fee_link"
            );
        }
    }
}