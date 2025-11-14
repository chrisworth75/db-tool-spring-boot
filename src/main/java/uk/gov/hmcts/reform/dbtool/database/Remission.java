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
@Table(name = "remission", schema = "payment")
@Data
@NoArgsConstructor
public class Remission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "fee_id")
    private Long feeId;

    @Size(max = 50)
    @Column(name = "hwf_reference", length = 50)
    private String hwfReference;

    @NotNull
    @Positive
    @Column(name = "hwf_amount", precision = 12, scale = 2)
    private BigDecimal hwfAmount;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Size(max = 25)
    @Column(name = "ccd_case_number", length = 25)
    private String ccdCaseNumber;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "remission_reference", length = 50)
    private String remissionReference;
}
