package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseSummary {
    private double totalFees;
    private double totalPayments;
    private double totalRefunds;
    private double totalRemissions;
    private int serviceRequestCount;
    private int feeCount;
    private int paymentCount;
    private int refundCount;
    private double netAmount;
    private double amountDue;
}
