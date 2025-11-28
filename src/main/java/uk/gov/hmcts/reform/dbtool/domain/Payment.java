package uk.gov.hmcts.reform.dbtool.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment - clean domain representation
 */
public record Payment(
    Long id,
    String reference,
    String amount,
    String currency,
    String status,
    String method,
    String provider,
    String channel,
    String externalReference,
    String customerReference,
    String pbaNumber,
    String payerName,
    LocalDateTime dateCreated,
    LocalDateTime dateUpdated,
    LocalDateTime bankedDate,
    List<Refund> refunds,
    List<Apportionment> apportionments
) {
    public Payment {
        refunds = refunds == null ? List.of() : List.copyOf(refunds);
        apportionments = apportionments == null ? List.of() : List.copyOf(apportionments);
    }
}