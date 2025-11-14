package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Remission - Help with Fees assistance
 */
@Data
@NoArgsConstructor
public class Remission {

    private String hwfReference;
    private Double amount;
    private String beneficiaryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Remission(String hwfReference, Double amount) {
        this.hwfReference = hwfReference;
        this.amount = amount;
    }
}
