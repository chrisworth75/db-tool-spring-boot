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
@Table(name = "fee_pay_apportion", schema = "public")
@Data
@NoArgsConstructor
public class ApportionmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "fee_id")
    private Long feeId;

    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "payment_amount", precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "apportion_amount", precision = 19, scale = 2)
    private BigDecimal apportionAmount;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "apportion_type")
    private String apportionType;

    @Column(name = "call_surplus_amount", precision = 19, scale = 2)
    private BigDecimal callSurplusAmount;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;
}
