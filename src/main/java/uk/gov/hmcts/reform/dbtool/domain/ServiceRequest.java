package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ServiceRequest - represents a payment_fee_link without database-specific fields
 */
public record ServiceRequest(
    Long id,
    String paymentReference,
    String ccdCaseNumber,
    String caseReference,
    List<Fee> fees,
    List<Payment> payments,
    LocalDateTime dateCreated,
    LocalDateTime dateUpdated,
    String orgId,
    String enterpriseServiceName,
    String serviceRequestCallbackUrl
) {
    public ServiceRequest {
        fees = fees == null ? List.of() : List.copyOf(fees);
        payments = payments == null ? List.of() : List.copyOf(payments);
    }
}