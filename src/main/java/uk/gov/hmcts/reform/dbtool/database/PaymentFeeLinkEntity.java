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
 * DATABASE MODEL - payment_fee_link table
 * Exact match to database structure with validation
 */
@Entity
@Table(name = "payment_fee_link", schema = "public")
@Data
@NoArgsConstructor
public class PaymentFeeLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @NotNull
    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    @NotNull
    @Size(max = 50)
    @Column(name = "payment_reference", length = 50)
    private String paymentReference;

    @Size(max = 20)
    @Column(name = "org_id", length = 20)
    private String orgId;

    @Size(max = 255)
    @Column(name = "enterprise_service_name", length = 255)
    private String enterpriseServiceName;

    @NotNull
    @Size(max = 25)
    @Column(name = "ccd_case_number", length = 25)
    private String ccdCaseNumber;

    @Size(max = 25)
    @Column(name = "case_reference", length = 25)
    private String caseReference;

    @Column(name = "service_request_callback_url")
    private String serviceRequestCallbackUrl;
}
