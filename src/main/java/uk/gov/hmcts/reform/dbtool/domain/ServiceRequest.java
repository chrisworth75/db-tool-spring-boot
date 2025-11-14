package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceRequest - represents a payment_fee_link without database-specific fields
 */
@Data
@NoArgsConstructor
public class ServiceRequest {

    private String paymentReference;
    private String ccdCaseNumber;
    private String caseReference;
    private String orgId;
    private String serviceName;
    private List<Fee> fees = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceRequest(String paymentReference, String ccdCaseNumber) {
        this.paymentReference = paymentReference;
        this.ccdCaseNumber = ccdCaseNumber;
    }

    public void addFee(Fee fee) {
        this.fees.add(fee);
    }

    public void addPayment(Payment payment) {
        this.payments.add(payment);
    }

    public List<Refund> getAllRefunds() {
        List<Refund> allRefunds = new ArrayList<>();
        for (Payment payment : payments) {
            allRefunds.addAll(payment.getRefunds());
        }
        return allRefunds;
    }

    public ServiceRequestSummary getSummary() {
        double totalFees = fees.stream()
                .mapToDouble(Fee::getTotalAmount)
                .sum();

        double totalRemissions = fees.stream()
                .flatMap(f -> f.getRemissions().stream())
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();

        double totalPayments = payments.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        double totalRefunds = payments.stream()
                .flatMap(p -> p.getRefunds().stream())
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();

        return ServiceRequestSummary.builder()
                .totalFees(totalFees)
                .totalPayments(totalPayments)
                .totalRefunds(totalRefunds)
                .totalRemissions(totalRemissions)
                .feeCount(fees.size())
                .paymentCount(payments.size())
                .refundCount(getAllRefunds().size())
                .netAmount(totalPayments + totalRemissions - totalRefunds)
                .amountDue(totalFees - totalPayments - totalRemissions)
                .build();
    }
}
