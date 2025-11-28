package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fee - clean domain representation
 */
public record Fee(
    Long id,
    String code,
    String version,
    String amount,
    String calculatedAmount,
    String netAmount,
    String amountDue,
    Integer volume,
    String reference,
    List<Remission> remissions,
    LocalDateTime dateCreated,
    LocalDateTime dateUpdated
) {
    public Fee {
        remissions = remissions == null ? List.of() : List.copyOf(remissions);
    }
}