package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refund - clean domain representation
 */
@Data
@NoArgsConstructor
public class Refund {

    private String reference;
    private Double amount;
    private String reason;
    private String status;
    private String instructionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public Refund(String reference, Double amount, String reason) {
        this.reference = reference;
        this.amount = amount;
        this.reason = reason;
    }
}
