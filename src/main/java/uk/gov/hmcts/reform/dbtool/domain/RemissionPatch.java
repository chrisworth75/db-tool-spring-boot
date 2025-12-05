package uk.gov.hmcts.reform.dbtool.domain;

/**
 * Patch DTO for Remission.
 * Uses hwfReference to uniquely identify the remission.
 */
public record RemissionPatch(
    String hwfReference
) {}