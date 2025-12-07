package uk.gov.hmcts.reform.dbtool.e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify the full cycle:
 * 1. Get initial state
 * 2. Generate SQL via PATCH
 * 3. Execute the SQL on databases
 * 4. Verify the changes
 * 5. Execute rollback SQL
 * 6. Verify state matches initial state
 */
class SqlExecutionAndRollbackIT extends BaseIT {

    @Autowired
    @Qualifier("paymentJdbcTemplate")
    private JdbcTemplate paymentJdbcTemplate;

    @Autowired
    @Qualifier("refundsJdbcTemplate")
    private JdbcTemplate refundsJdbcTemplate;

    @Test
    @DisplayName("DELETE and ROLLBACK: Should delete entities and restore them with rollback SQL")
    void shouldDeleteAndRollbackEntities() {
        String ccdCaseNumber = "1000000000000001";

        // Step 1: Capture initial state
        Map<String, Object> initialState = captureState(ccdCaseNumber);
        int initialFeeCount = (int) initialState.get("feeCount");
        int initialPaymentCount = (int) initialState.get("paymentCount");
        int initialApportionmentCount = (int) initialState.get("apportionmentCount");
        int initialLinkCount = (int) initialState.get("linkCount");

        assertTrue(initialFeeCount > 0, "Should have fees initially");
        assertTrue(initialPaymentCount > 0, "Should have payments initially");

        // Step 2: Generate deletion SQL via PATCH (delete everything)
        String patchBody = """
            {
                "ccdCaseNumber": "%s",
                "serviceRequests": []
            }
            """.formatted(ccdCaseNumber);

        Response patchResponse = given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
            .patch("/cases/ccd/" + ccdCaseNumber);

        assertEquals(200, patchResponse.statusCode());

        List<String> deleteSql = patchResponse.jsonPath().getList("paymentDatabaseSql", String.class);
        List<String> rollbackSql = patchResponse.jsonPath().getList("paymentDatabaseRollbackSql", String.class);

        assertFalse(deleteSql.isEmpty(), "Should have DELETE SQL statements");
        assertFalse(rollbackSql.isEmpty(), "Should have rollback SQL statements");

        // Step 3: Execute DELETE SQL
        for (String sql : deleteSql) {
            paymentJdbcTemplate.execute(sql);
        }

        // Step 4: Verify entities were deleted
        Map<String, Object> afterDeleteState = captureState(ccdCaseNumber);
        assertEquals(0, afterDeleteState.get("feeCount"), "Fees should be deleted");
        assertEquals(0, afterDeleteState.get("paymentCount"), "Payments should be deleted");
        assertEquals(0, afterDeleteState.get("apportionmentCount"), "Apportionments should be deleted");
        assertEquals(0, afterDeleteState.get("linkCount"), "Links should be deleted");

        // Step 5: Execute ROLLBACK SQL
        for (String sql : rollbackSql) {
            paymentJdbcTemplate.execute(sql);
        }

        // Step 6: Verify state matches initial state
        Map<String, Object> afterRollbackState = captureState(ccdCaseNumber);
        assertEquals(initialFeeCount, afterRollbackState.get("feeCount"), "Fee count should match initial state");
        assertEquals(initialPaymentCount, afterRollbackState.get("paymentCount"), "Payment count should match initial state");
        assertEquals(initialApportionmentCount, afterRollbackState.get("apportionmentCount"), "Apportionment count should match initial state");
        assertEquals(initialLinkCount, afterRollbackState.get("linkCount"), "Link count should match initial state");
    }

    @Test
    @DisplayName("DELETE and ROLLBACK: Should delete refunds from separate database and restore them")
    void shouldDeleteAndRollbackRefunds() {
        String ccdCaseNumber = "1000000000000004";

        // Step 1: Capture initial refund count
        int initialRefundCount = countRefundsForCase(ccdCaseNumber);
        assertTrue(initialRefundCount > 0, "Should have refunds initially");

        // Step 2: Generate deletion SQL (delete refund but keep payment)
        String patchBody = """
            {
                "ccdCaseNumber": "%s",
                "serviceRequests": [
                    {
                        "id": 4,
                        "fees": [{"id": 5}],
                        "payments": [{"id": 5, "refunds": [], "apportionments": [{"id": 5}]}]
                    }
                ]
            }
            """.formatted(ccdCaseNumber);

        Response patchResponse = given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
            .patch("/cases/ccd/" + ccdCaseNumber);

        assertEquals(200, patchResponse.statusCode());

        List<String> refundsDeleteSql = patchResponse.jsonPath().getList("refundsDatabaseSql", String.class);
        List<String> refundsRollbackSql = patchResponse.jsonPath().getList("refundsDatabaseRollbackSql", String.class);

        assertFalse(refundsDeleteSql.isEmpty(), "Should have refunds DELETE SQL");
        assertFalse(refundsRollbackSql.isEmpty(), "Should have refunds rollback SQL");

        // Step 3: Execute DELETE SQL on refunds database
        for (String sql : refundsDeleteSql) {
            refundsJdbcTemplate.execute(sql);
        }

        // Step 4: Verify refund was deleted
        int afterDeleteCount = countRefundsForCase(ccdCaseNumber);
        assertEquals(0, afterDeleteCount, "Refunds should be deleted");

        // Step 5: Execute ROLLBACK SQL
        for (String sql : refundsRollbackSql) {
            refundsJdbcTemplate.execute(sql);
        }

        // Step 6: Verify refund count matches initial
        int afterRollbackCount = countRefundsForCase(ccdCaseNumber);
        assertEquals(initialRefundCount, afterRollbackCount, "Refund count should match initial state");
    }

    @Test
    @DisplayName("MOVE and ROLLBACK: Should move entities and restore original positions with rollback")
    void shouldMoveAndRollbackEntities() {
        String ccdCaseNumber = "1000000000000005";

        // Step 1: Capture initial state - fees on each link
        int feesOnLink5 = countFeesOnLink(5L);
        int feesOnLink6 = countFeesOnLink(6L);

        assertTrue(feesOnLink5 > 0, "Link 5 should have fees");
        assertTrue(feesOnLink6 > 0, "Link 6 should have fees");

        // Step 2: Generate SQL to move entities from link 6 to link 5, then delete link 6
        String patchBody = """
            {
                "ccdCaseNumber": "%s",
                "serviceRequests": [
                    {
                        "id": 5,
                        "fees": [
                            {"id": 6, "remissions": [{"hwfReference": "HWF-MOVE-001"}, {"hwfReference": "HWF-MOVE-002"}]},
                            {"id": 7, "remissions": []},
                            {"id": 8, "remissions": []}
                        ],
                        "payments": [
                            {"id": 6, "apportionments": [{"id": 6}, {"id": 7}, {"id": 8}]},
                            {"id": 7, "apportionments": []}
                        ]
                    }
                ]
            }
            """.formatted(ccdCaseNumber);

        Response patchResponse = given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
            .patch("/cases/ccd/" + ccdCaseNumber);

        assertEquals(200, patchResponse.statusCode());

        // Verify move counts
        int feesToMove = patchResponse.jsonPath().getInt("summary.feesToMove");
        int paymentsToMove = patchResponse.jsonPath().getInt("summary.paymentsToMove");

        assertTrue(feesToMove > 0, "Should have fees to move");

        List<String> sql = patchResponse.jsonPath().getList("paymentDatabaseSql", String.class);
        List<String> rollbackSql = patchResponse.jsonPath().getList("paymentDatabaseRollbackSql", String.class);

        // Step 3: Execute SQL (moves and deletions)
        for (String statement : sql) {
            paymentJdbcTemplate.execute(statement);
        }

        // Step 4: Verify moves occurred
        int feesOnLink5AfterMove = countFeesOnLink(5L);
        int feesOnLink6AfterMove = countFeesOnLink(6L);

        assertEquals(feesOnLink5 + feesOnLink6, feesOnLink5AfterMove,
            "All fees should now be on link 5");
        assertEquals(0, feesOnLink6AfterMove,
            "Link 6 should have no fees (should be deleted)");

        // Step 5: Execute rollback SQL
        for (String statement : rollbackSql) {
            paymentJdbcTemplate.execute(statement);
        }

        // Step 6: Verify state restored
        int feesOnLink5AfterRollback = countFeesOnLink(5L);
        int feesOnLink6AfterRollback = countFeesOnLink(6L);

        assertEquals(feesOnLink5, feesOnLink5AfterRollback, "Fees on link 5 should match initial");
        assertEquals(feesOnLink6, feesOnLink6AfterRollback, "Fees on link 6 should match initial");
    }

    @Test
    @DisplayName("Partial DELETE: Should delete some entities and rollback correctly")
    void shouldPartiallyDeleteAndRollback() {
        String ccdCaseNumber = "1000000000000002";

        // Step 1: Capture initial state
        int initialFeeCount = countFeesForCase(ccdCaseNumber);
        assertTrue(initialFeeCount >= 2, "Should have at least 2 fees");

        // Step 2: Generate SQL to delete one fee but keep the other
        String patchBody = """
            {
                "ccdCaseNumber": "%s",
                "serviceRequests": [
                    {
                        "id": 2,
                        "fees": [{"id": 2}],
                        "payments": [{"id": 2}, {"id": 3}]
                    }
                ]
            }
            """.formatted(ccdCaseNumber);

        Response patchResponse = given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
            .patch("/cases/ccd/" + ccdCaseNumber);

        assertEquals(200, patchResponse.statusCode());

        int feesToDelete = patchResponse.jsonPath().getInt("summary.feesToDelete");
        assertEquals(1, feesToDelete, "Should delete exactly 1 fee");

        List<String> sql = patchResponse.jsonPath().getList("paymentDatabaseSql", String.class);
        List<String> rollbackSql = patchResponse.jsonPath().getList("paymentDatabaseRollbackSql", String.class);

        // Step 3: Execute DELETE SQL
        for (String statement : sql) {
            paymentJdbcTemplate.execute(statement);
        }

        // Step 4: Verify partial deletion
        int afterDeleteCount = countFeesForCase(ccdCaseNumber);
        assertEquals(initialFeeCount - 1, afterDeleteCount, "One fee should be deleted");

        // Step 5: Execute rollback
        for (String statement : rollbackSql) {
            paymentJdbcTemplate.execute(statement);
        }

        // Step 6: Verify restoration
        int afterRollbackCount = countFeesForCase(ccdCaseNumber);
        assertEquals(initialFeeCount, afterRollbackCount, "Fee count should match initial");
    }

    @Test
    @DisplayName("No changes: Empty SQL should produce no effect")
    void shouldProduceNoEffectWhenNoChanges() {
        String ccdCaseNumber = "1000000000000001";

        // Step 1: Capture initial state
        Map<String, Object> initialState = captureState(ccdCaseNumber);

        // Step 2: Generate SQL keeping everything
        String patchBody = """
            {
                "ccdCaseNumber": "%s",
                "serviceRequests": [
                    {
                        "id": 1,
                        "fees": [{"id": 1}],
                        "payments": [{"id": 1, "apportionments": [{"id": 1}]}]
                    }
                ]
            }
            """.formatted(ccdCaseNumber);

        Response patchResponse = given()
            .contentType(ContentType.JSON)
            .body(patchBody)
            .when()
            .patch("/cases/ccd/" + ccdCaseNumber);

        assertEquals(200, patchResponse.statusCode());

        List<String> sql = patchResponse.jsonPath().getList("paymentDatabaseSql", String.class);
        List<String> rollbackSql = patchResponse.jsonPath().getList("paymentDatabaseRollbackSql", String.class);

        assertTrue(sql.isEmpty(), "Should have no SQL when keeping everything");
        assertTrue(rollbackSql.isEmpty(), "Should have no rollback SQL when keeping everything");

        // Verify state unchanged
        Map<String, Object> afterState = captureState(ccdCaseNumber);
        assertEquals(initialState, afterState, "State should be unchanged");
    }

    // Helper methods

    private Map<String, Object> captureState(String ccdCaseNumber) {
        int linkCount = paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payment_fee_link WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);
        int feeCount = paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fee WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);
        int paymentCount = paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payment WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);
        int apportionmentCount = paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fee_pay_apportion WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);

        return Map.of(
            "linkCount", linkCount,
            "feeCount", feeCount,
            "paymentCount", paymentCount,
            "apportionmentCount", apportionmentCount
        );
    }

    private int countRefundsForCase(String ccdCaseNumber) {
        return refundsJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refunds WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);
    }

    private int countFeesForCase(String ccdCaseNumber) {
        return paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fee WHERE ccd_case_number = ?",
            Integer.class, ccdCaseNumber);
    }

    private int countFeesOnLink(Long paymentLinkId) {
        return paymentJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fee WHERE payment_link_id = ?",
            Integer.class, paymentLinkId);
    }
}