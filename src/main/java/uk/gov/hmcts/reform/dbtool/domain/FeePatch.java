package uk.gov.hmcts.reform.dbtool.domain;

import java.util.List;

/**
 * Patch DTO for Fee.
 * Uses id to uniquely identify the fee.
 */
public record FeePatch(
    Long id,
    List<RemissionPatch> remissions
) {
    public FeePatch {
        remissions = remissions == null ? List.of() : List.copyOf(remissions);
    }
}