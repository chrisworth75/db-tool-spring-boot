package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;

/**
 * Refund - clean domain representation
 */
public record Refund(
    String reference,
    Double amount,
    String reason,
    String status,
    String instructionType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}