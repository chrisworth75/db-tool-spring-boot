package uk.gov.hmcts.reform.dbtool.database;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DATABASE MODEL - remission table
 */
@Entity
@Table(name = "remission", schema = "public")
@Data
@NoArgsConstructor
public class RemissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_id")
    private Long feeId;

    @Column(name = "hwf_reference")
    private String hwfReference;

    @Column(name = "hwf_amount", precision = 19, scale = 2)
    private BigDecimal hwfAmount;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "case_reference")
    private String caseReference;

    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @Column(name = "site_id")
    private String siteId;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "remission_reference")
    private String remissionReference;
}
