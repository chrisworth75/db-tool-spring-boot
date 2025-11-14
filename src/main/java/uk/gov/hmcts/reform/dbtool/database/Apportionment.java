package uk.gov.hmcts.reform.dbtool.database;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DATABASE MODEL - fee_pay_apportion table
 */
@Entity
@Table(name = "fee_pay_apportion", schema = "payment")
@Data
@NoArgsConstructor
public class Apportionment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "payment_id")
    private Long paymentId;

    @NotNull
    @Column(name = "fee_id")
    private Long feeId;

    @PositiveOrZero
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "payment_amount", precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Size(max = 25)
    @Column(name = "ccd_case_number", length = 25)
    private String ccdCaseNumber;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "apportion_type")
    private String apportionType;

    @Column(name = "call_surplus_amount", precision = 12, scale = 2)
    private BigDecimal callSurplusAmount;

    @PositiveOrZero
    @Column(name = "apportion_amount", precision = 12, scale = 2)
    private BigDecimal apportionAmount;
}
