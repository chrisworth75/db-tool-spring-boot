package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * CLEAN DOMAIN MODEL
 * Represents a legal case with no database-specific fields.
 * Optimized for human understanding and business logic.
 */
@Data
@NoArgsConstructor
public class Case {

    private String ccdCaseNumber;
    private List<ServiceRequest> serviceRequests = new ArrayList<>();

    public Case(String ccdCaseNumber) {
        this.ccdCaseNumber = ccdCaseNumber;
    }

    public void addServiceRequest(ServiceRequest sr) {
        this.serviceRequests.add(sr);
    }

    public CaseSummary getSummary() {
        int totalFees = 0;
        int totalPayments = 0;
        int totalRefunds = 0;
        int totalRemissions = 0;
        int feeCount = 0;
        int paymentCount = 0;
        int refundCount = 0;
        int remissionCount = 0;

        for (ServiceRequest sr : serviceRequests) {
            for (Fee fee : sr.fees()) {
                if (fee.amount() != null) {
                    totalFees += parseAmount(fee.amount());
                }
                feeCount++;
                remissionCount += fee.remissions().size();
                for (Remission rem : fee.remissions()) {
                    if (rem.amount() != null) {
                        totalRemissions += rem.amount().intValue();
                    }
                }
            }
            for (Payment payment : sr.payments()) {
                if (payment.amount() != null) {
                    totalPayments += parseAmount(payment.amount());
                }
                paymentCount++;
                for (Refund refund : payment.refunds()) {
                    if (refund.amount() != null) {
                        totalRefunds += refund.amount().intValue();
                    }
                    refundCount++;
                }
            }
        }

        return CaseSummary.builder()
                .totalFees(totalFees)
                .totalPayments(totalPayments)
                .totalRefunds(totalRefunds)
                .totalRemissions(totalRemissions)
                .serviceRequestCount(serviceRequests.size())
                .feeCount(feeCount)
                .paymentCount(paymentCount)
                .refundCount(refundCount)
                .remissionCount(remissionCount)
                .netAmount(totalPayments + totalRemissions - totalRefunds)
                .amountDue(totalFees - totalPayments - totalRemissions)
                .build();
    }

    private int parseAmount(String amount) {
        try {
            return (int) Double.parseDouble(amount);
        } catch (Exception e) {
            return 0;
        }
    }
}