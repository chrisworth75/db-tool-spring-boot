package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;

/**
 * Apportionment - represents fee allocation to payment
 */
public record Apportionment(
    Long id,
    Long feeId,
    String apportionAmount,
    String apportionType,
    String callSurplusAmount,
    LocalDateTime dateCreated,
    LocalDateTime dateUpdated
) {}