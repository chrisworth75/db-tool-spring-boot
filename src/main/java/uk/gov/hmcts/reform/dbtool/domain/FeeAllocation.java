package uk.gov.hmcts.reform.dbtool.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents how a payment is allocated to a fee (clean model)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeAllocation {
    private String feeCode;
    private Double amount;
}
