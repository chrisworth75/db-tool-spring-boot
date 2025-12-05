package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Patch DTO for Payment.
 * Uses id or reference to uniquely identify the payment.
 */
public record PaymentPatch(
    Long id,
    String reference,
    List<RefundPatch> refunds,
    List<ApportionmentPatch> apportionments
) {
    public PaymentPatch {
        refunds = refunds == null ? List.of() : List.copyOf(refunds);
        apportionments = apportionments == null ? List.of() : List.copyOf(apportionments);
    }
}