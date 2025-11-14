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
 * DATABASE MODEL - payment table
 */
@Entity
@Table(name = "payment", schema = "payment")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "case_reference")
    private String caseReference;

    @NotNull
    @Size(max = 25)
    @Column(name = "ccd_case_number", length = 25)
    private String ccdCaseNumber;

    @NotNull
    @Size(max = 3)
    @Column(name = "currency", length = 3)
    private String currency = "GBP";

    @NotNull
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @NotNull
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @Column(name = "description")
    private String description;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "site_id")
    private String siteId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "payment_channel")
    private String paymentChannel;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "payment_status")
    private String paymentStatus;

    @NotNull
    @Column(name = "payment_link_id")
    private Long paymentLinkId;

    @Column(name = "customer_reference")
    private String customerReference;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "pba_number")
    private String pbaNumber;

    @NotNull
    @Size(max = 50)
    @Column(name = "reference", length = 50)
    private String reference;

    @Column(name = "giro_slip_no")
    private String giroSlipNo;

    @Column(name = "s2s_service_name")
    private String s2sServiceName;

    @Column(name = "reported_date_offline")
    private LocalDateTime reportedDateOffline;

    @Column(name = "service_callback_url")
    private String serviceCallbackUrl;

    @Column(name = "document_control_number")
    private String documentControlNumber;

    @Column(name = "banked_date")
    private LocalDateTime bankedDate;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "internal_reference")
    private String internalReference;
}
