package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Request DTO for PATCH endpoint.
 * Uses the same ServiceRequest/Fee/Payment DTOs as the GET response.
 * Any service requests present in the received DTO will be kept; those absent will be deleted.
 * Fields can be null/omitted if only identifying entities for deletion.
 * In future, non-null fields can be used to update entity values.
 */
public record CasePatchRequest(
    String ccdCaseNumber,
    List<ServiceRequest> serviceRequests
) {
    public CasePatchRequest {
        serviceRequests = serviceRequests == null ? List.of() : List.copyOf(serviceRequests);
    }
}