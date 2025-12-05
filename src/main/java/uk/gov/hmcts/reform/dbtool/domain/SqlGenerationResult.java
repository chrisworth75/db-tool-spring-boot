package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Result of SQL generation from a PATCH request comparison.
 * Contains the SQL statements to execute and a summary of changes.
 */
public record SqlGenerationResult(
    List<String> paymentDatabaseSql,
    List<String> refundsDatabaseSql,
    DeletionSummary summary
) {
    public SqlGenerationResult {
        paymentDatabaseSql = paymentDatabaseSql == null ? List.of() : List.copyOf(paymentDatabaseSql);
        refundsDatabaseSql = refundsDatabaseSql == null ? List.of() : List.copyOf(refundsDatabaseSql);
    }

    public record DeletionSummary(
        int serviceRequestsToDelete,
        int feesToDelete,
        int paymentsToDelete,
        int remissionsToDelete,
        int refundsToDelete,
        int apportionmentsToDelete
    ) {}
}