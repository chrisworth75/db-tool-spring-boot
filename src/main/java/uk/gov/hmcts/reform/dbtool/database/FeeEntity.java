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
@Table(name = "fee", schema = "public")
@Data
@NoArgsConstructor
public class FeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "version")
    private String version;

    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @Column(name = "calculated_amount", precision = 19, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "reference")
    private String reference;

    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "amount_due", precision = 19, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;
}
