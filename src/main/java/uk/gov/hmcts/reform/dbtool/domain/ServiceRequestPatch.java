package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Patch DTO for ServiceRequest (payment_fee_link).
 * Uses id or paymentReference to uniquely identify the service request.
 * Nested lists indicate what to keep - absent items will be deleted.
 */
public record ServiceRequestPatch(
    Long id,
    String paymentReference,
    List<FeePatch> fees,
    List<PaymentPatch> payments
) {
    public ServiceRequestPatch {
        fees = fees == null ? List.of() : List.copyOf(fees);
        payments = payments == null ? List.of() : List.copyOf(payments);
    }
}