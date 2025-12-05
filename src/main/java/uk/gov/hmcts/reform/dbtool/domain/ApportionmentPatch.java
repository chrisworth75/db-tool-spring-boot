package uk.gov.hmcts.reform.dbtool.domain;

/**
 * Patch DTO for Apportionment.
 * Uses id to uniquely identify the apportionment.
 */
public record ApportionmentPatch(
    Long id
) {}