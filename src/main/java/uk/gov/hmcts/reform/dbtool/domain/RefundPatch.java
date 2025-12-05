package uk.gov.hmcts.reform.dbtool.domain;

/**
 * Patch DTO for Refund.
 * Uses reference to uniquely identify the refund.
 */
public record RefundPatch(
    String reference
) {}