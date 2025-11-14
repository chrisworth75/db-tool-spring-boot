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
 * DATABASE MODEL - fee table
 */
@Entity
@Table(name = "fee", schema = "payment")
@Data
@NoArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "code", length = 20)
    private String code;

    @NotNull
    @Column(name = "version")
    private String version;

    @NotNull
    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @NotNull
    @Positive
    @Column(name = "calculated_amount", precision = 12, scale = 2)
    private BigDecimal calculatedAmount;

    @NotNull
    @Positive
    @Column(name = "volume")
    private Integer volume;

    @NotNull
    @Size(max = 25)
    @Column(name = "ccd_case_number", length = 25)
    private String ccdCaseNumber;

    @Column(name = "reference")
    private String reference;

    @NotNull
    @Positive
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @NotNull
    @Positive
    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @NotNull
    @Positive
    @Column(name = "amount_due", precision = 12, scale = 2)
    private BigDecimal amountDue;

    @NotNull
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @NotNull
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;
}
