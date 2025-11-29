package uk.gov.hmcts.reform.dbtool.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end tests for the Case API using RestAssured and Testcontainers.
 */
class CaseApiE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("GET /api/cases/ccd/{ccdCaseNumber}")
    class GetCaseByCcd {

        @Test
        @DisplayName("should return case with single fee and payment (fully paid)")
        void shouldReturnSimpleCase() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000001")
                .then()
                    .statusCode(200)
                    .body("case.ccdCaseNumber", equalTo("1000000000000001"))
                    .body("case.serviceRequests", hasSize(1))
                    .body("case.serviceRequests[0].paymentReference", equalTo("PAY-TEST-001"))
                    .body("case.serviceRequests[0].fees", hasSize(1))
                    .body("case.serviceRequests[0].fees[0].code", equalTo("FEE0001"))
                    .body("case.serviceRequests[0].fees[0].amount", equalTo("100.00"))
                    .body("case.serviceRequests[0].payments", hasSize(1))
                    .body("case.serviceRequests[0].payments[0].reference", equalTo("RC-TEST-0001"))
                    .body("case.serviceRequests[0].payments[0].amount", equalTo("100.00"))
                    .body("case.serviceRequests[0].payments[0].status", equalTo("success"))
                    .body("summary.totalFees", equalTo(100))
                    .body("summary.totalPayments", equalTo(100))
                    .body("summary.amountDue", equalTo(0));
        }

        @Test
        @DisplayName("should return case with multiple fees and payments")
        void shouldReturnCaseWithMultipleFees() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000002")
                .then()
                    .statusCode(200)
                    .body("case.ccdCaseNumber", equalTo("1000000000000002"))
                    .body("case.serviceRequests[0].fees", hasSize(2))
                    .body("case.serviceRequests[0].payments", hasSize(2))
                    .body("summary.feeCount", equalTo(2))
                    .body("summary.paymentCount", equalTo(2))
                    .body("summary.totalFees", equalTo(225)) // 150 + 75
                    .body("summary.totalPayments", equalTo(175)); // 100 + 75
        }

        @Test
        @DisplayName("should return case with remission (Help with Fees)")
        void shouldReturnCaseWithRemission() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000003")
                .then()
                    .statusCode(200)
                    .body("case.ccdCaseNumber", equalTo("1000000000000003"))
                    .body("case.serviceRequests[0].fees[0].remissions", hasSize(1))
                    .body("case.serviceRequests[0].fees[0].remissions[0].hwfReference", equalTo("HWF-123-456"))
                    .body("case.serviceRequests[0].fees[0].remissions[0].amount", equalTo(50.0F))
                    .body("case.serviceRequests[0].fees[0].remissions[0].beneficiaryName", equalTo("John Smith"))
                    .body("summary.remissionCount", equalTo(1))
                    .body("summary.totalRemissions", equalTo(50));
        }

        @Test
        @DisplayName("should return case with refund from separate database")
        void shouldReturnCaseWithRefund() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000004")
                .then()
                    .statusCode(200)
                    .body("case.ccdCaseNumber", equalTo("1000000000000004"))
                    .body("case.serviceRequests[0].payments[0].refunds", hasSize(1))
                    .body("case.serviceRequests[0].payments[0].refunds[0].reference", equalTo("RF-TEST-0001"))
                    .body("case.serviceRequests[0].payments[0].refunds[0].amount", equalTo(50.0F))
                    .body("case.serviceRequests[0].payments[0].refunds[0].reason", equalTo("Overpayment"))
                    .body("case.serviceRequests[0].payments[0].refunds[0].status", equalTo("Approved"))
                    .body("summary.refundCount", equalTo(1))
                    .body("summary.totalRefunds", equalTo(50));
        }

        @Test
        @DisplayName("should return 404 for non-existent case")
        void shouldReturn404ForNonExistentCase() {
            given()
                .when()
                    .get("/cases/ccd/9999999999999999")
                .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("should return correct summary calculations")
        void shouldReturnCorrectSummaryCalculations() {
            // Case 4: 300 fee, 300 payment, 50 refund
            // netAmount = payments + remissions - refunds = 300 + 0 - 50 = 250
            // amountDue = fees - payments - remissions = 300 - 300 - 0 = 0
            given()
                .when()
                    .get("/cases/ccd/1000000000000004")
                .then()
                    .statusCode(200)
                    .body("summary.totalFees", equalTo(300))
                    .body("summary.totalPayments", equalTo(300))
                    .body("summary.totalRefunds", equalTo(50))
                    .body("summary.totalRemissions", equalTo(0))
                    .body("summary.netAmount", equalTo(250))
                    .body("summary.amountDue", equalTo(0));
        }

        @Test
        @DisplayName("should return apportionments on payments")
        void shouldReturnApportionments() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000001")
                .then()
                    .statusCode(200)
                    .body("case.serviceRequests[0].payments[0].apportionments", hasSize(1))
                    .body("case.serviceRequests[0].payments[0].apportionments[0].apportionAmount", equalTo("100.00"))
                    .body("case.serviceRequests[0].payments[0].apportionments[0].apportionType", equalTo("AUTO"));
        }
    }

    @Nested
    @DisplayName("GET /api/cases/ccd/{ccdCaseNumber}/summary")
    class GetCaseSummary {

        @Test
        @DisplayName("should return summary only for existing case")
        void shouldReturnSummaryOnly() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000001/summary")
                .then()
                    .statusCode(200)
                    .body("totalFees", equalTo(100))
                    .body("totalPayments", equalTo(100))
                    .body("totalRefunds", equalTo(0))
                    .body("totalRemissions", equalTo(0))
                    .body("serviceRequestCount", equalTo(1))
                    .body("feeCount", equalTo(1))
                    .body("paymentCount", equalTo(1))
                    .body("refundCount", equalTo(0))
                    .body("remissionCount", equalTo(0))
                    .body("netAmount", equalTo(100))
                    .body("amountDue", equalTo(0));
        }

        @Test
        @DisplayName("should return 404 for non-existent case")
        void shouldReturn404ForNonExistentCase() {
            given()
                .when()
                    .get("/cases/ccd/9999999999999999/summary")
                .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("should return summary with remission included")
        void shouldReturnSummaryWithRemission() {
            // Case 3: 200 fee, 150 payment, 50 remission
            given()
                .when()
                    .get("/cases/ccd/1000000000000003/summary")
                .then()
                    .statusCode(200)
                    .body("totalFees", equalTo(200))
                    .body("totalPayments", equalTo(150))
                    .body("totalRemissions", equalTo(50))
                    .body("remissionCount", equalTo(1));
        }
    }

    @Nested
    @DisplayName("Response structure validation")
    class ResponseStructure {

        @Test
        @DisplayName("should return proper JSON structure for case")
        void shouldReturnProperJsonStructure() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000001")
                .then()
                    .statusCode(200)
                    .body("case", notNullValue())
                    .body("summary", notNullValue())
                    .body("case.ccdCaseNumber", notNullValue())
                    .body("case.serviceRequests", notNullValue());
        }

        @Test
        @DisplayName("should return service request with all expected fields")
        void shouldReturnServiceRequestWithAllFields() {
            given()
                .when()
                    .get("/cases/ccd/1000000000000001")
                .then()
                    .statusCode(200)
                    .body("case.serviceRequests[0].id", notNullValue())
                    .body("case.serviceRequests[0].paymentReference", notNullValue())
                    .body("case.serviceRequests[0].ccdCaseNumber", notNullValue())
                    .body("case.serviceRequests[0].caseReference", notNullValue())
                    .body("case.serviceRequests[0].fees", notNullValue())
                    .body("case.serviceRequests[0].payments", notNullValue())
                    .body("case.serviceRequests[0].orgId", notNullValue())
                    .body("case.serviceRequests[0].enterpriseServiceName", notNullValue());
        }
    }
}