package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;

/**
 * Remission - Help with Fees assistance
 */
public record Remission(
    String hwfReference,
    Double amount,
    String beneficiaryName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}