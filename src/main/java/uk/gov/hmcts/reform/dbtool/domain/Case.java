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
        double totalFees = 0;
        double totalPayments = 0;
        double totalRefunds = 0;
        double totalRemissions = 0;
        int feeCount = 0;
        int paymentCount = 0;
        int refundCount = 0;

        for (ServiceRequest sr : serviceRequests) {
            ServiceRequestSummary srSummary = sr.getSummary();
            totalFees += srSummary.getTotalFees();
            totalPayments += srSummary.getTotalPayments();
            totalRefunds += srSummary.getTotalRefunds();
            totalRemissions += srSummary.getTotalRemissions();
            feeCount += srSummary.getFeeCount();
            paymentCount += srSummary.getPaymentCount();
            refundCount += srSummary.getRefundCount();
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
                .netAmount(totalPayments + totalRemissions - totalRefunds)
                .amountDue(totalFees - totalPayments - totalRemissions)
                .build();
    }
}
