package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Request DTO for PATCH endpoint.
 * Contains only the identifiers needed to locate entities and determine what to keep/delete.
 * Any service requests present in the received DTO will be kept; those absent will be deleted.
 */
public record CasePatchRequest(
    String ccdCaseNumber,
    List<ServiceRequestPatch> serviceRequests
) {
    public CasePatchRequest {
        serviceRequests = serviceRequests == null ? List.of() : List.copyOf(serviceRequests);
    }
}