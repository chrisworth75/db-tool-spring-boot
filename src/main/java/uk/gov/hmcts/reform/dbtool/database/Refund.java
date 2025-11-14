package uk.gov.hmcts.reform.dbtool.database;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DATABASE MODEL - refunds table
 */
@Entity
@Table(name = "refunds", schema = "refunds")
@Data
@NoArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Positive
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "refund_status")
    private String refundStatus;

    @Size(max = 255)
    @Column(name = "reference", length = 255)
    private String reference;

    @Size(max = 255)
    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Size(max = 255)
    @Column(name = "ccd_case_number", length = 255)
    private String ccdCaseNumber;

    @Column(name = "fee_ids")
    private String feeIds;

    @Column(name = "notification_sent_flag")
    private String notificationSentFlag;

    @Column(name = "contact_details")
    private String contactDetails;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "refund_instruction_type")
    private String refundInstructionType;
}
