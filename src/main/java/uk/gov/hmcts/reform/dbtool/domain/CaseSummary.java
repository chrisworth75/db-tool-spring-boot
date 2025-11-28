package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Builder;

@Builder
public record CaseSummary(
    int totalFees,
    int totalPayments,
    int totalRefunds,
    int totalRemissions,
    int serviceRequestCount,
    int feeCount,
    int paymentCount,
    int refundCount,
    int remissionCount,
    int netAmount,
    int amountDue
) {}