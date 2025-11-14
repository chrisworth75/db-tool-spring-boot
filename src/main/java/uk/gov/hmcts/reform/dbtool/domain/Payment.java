package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment - clean domain representation
 */
@Data
@NoArgsConstructor
public class Payment {

    private String reference;
    private Double amount;
    private String currency = "GBP";
    private String status;
    private String method;
    private String provider;
    private String channel;
    private String customerReference;
    private String pbaNumber;
    private String payerName;
    private List<Refund> refunds = new ArrayList<>();
    private List<FeeAllocation> feeAllocations = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime bankedAt;

    public Payment(String reference, Double amount) {
        this.reference = reference;
        this.amount = amount;
    }

    public void addRefund(Refund refund) {
        this.refunds.add(refund);
    }

    public void addFeeAllocation(String feeCode, Double amount) {
        this.feeAllocations.add(new FeeAllocation(feeCode, amount));
    }

    public double getTotalRefunded() {
        return refunds.stream()
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();
    }

    public double getNetAmount() {
        return (amount != null ? amount : 0.0) - getTotalRefunded();
    }
}
