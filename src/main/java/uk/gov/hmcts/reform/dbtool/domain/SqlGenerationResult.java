package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Result of SQL generation from a PATCH request comparison.
 * Contains DELETE, UPDATE (for moves), and rollback INSERT statements.
 */
public record SqlGenerationResult(
    List<String> paymentDatabaseSql,
    List<String> refundsDatabaseSql,
    List<String> paymentDatabaseRollbackSql,
    List<String> refundsDatabaseRollbackSql,
    ChangeSummary summary
) {
    public SqlGenerationResult {
        paymentDatabaseSql = paymentDatabaseSql == null ? List.of() : List.copyOf(paymentDatabaseSql);
        refundsDatabaseSql = refundsDatabaseSql == null ? List.of() : List.copyOf(refundsDatabaseSql);
        paymentDatabaseRollbackSql = paymentDatabaseRollbackSql == null ? List.of() : List.copyOf(paymentDatabaseRollbackSql);
        refundsDatabaseRollbackSql = refundsDatabaseRollbackSql == null ? List.of() : List.copyOf(refundsDatabaseRollbackSql);
    }

    public record ChangeSummary(
        int serviceRequestsToDelete,
        int feesToDelete,
        int paymentsToDelete,
        int remissionsToDelete,
        int refundsToDelete,
        int apportionmentsToDelete,
        int feesToMove,
        int paymentsToMove,
        int remissionsToMove,
        int apportionmentsToMove
    ) {
        // Convenience constructor for backwards compatibility
        public ChangeSummary(
            int serviceRequestsToDelete,
            int feesToDelete,
            int paymentsToDelete,
            int remissionsToDelete,
            int refundsToDelete,
            int apportionmentsToDelete
        ) {
            this(serviceRequestsToDelete, feesToDelete, paymentsToDelete,
                 remissionsToDelete, refundsToDelete, apportionmentsToDelete,
                 0, 0, 0, 0);
        }
    }

    // Backwards compatibility alias
    public ChangeSummary summary() {
        return summary;
    }
}