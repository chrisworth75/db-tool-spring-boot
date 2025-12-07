package uk.gov.hmcts.reform.dbtool.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.*;
import uk.gov.hmcts.reform.dbtool.repository.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comparing PATCH request DTOs with database state
 * and generating SQL statements for deletions and moves.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseDiffService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final RemissionRepository remissionRepository;
    private final RefundRepository refundRepository;
    private final ApportionmentRepository apportionmentRepository;

    /**
     * Compare the patch request with the database state and generate SQL for deletions and moves.
     * Items present in the patch request are kept; absent items are deleted.
     * Items that appear under a different service request than in the database will be moved.
     */
    @Transactional(readOnly = true)
    public SqlGenerationResult generateDeletionSql(CasePatchRequest patchRequest) {
        String ccdCaseNumber = patchRequest.ccdCaseNumber();
        log.info("Generating SQL for CCD: {}", ccdCaseNumber);

        // Fetch current database state
        List<PaymentFeeLinkEntity> dbLinks = paymentFeeLinkRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<FeeEntity> dbFees = feeRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<PaymentEntity> dbPayments = paymentRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<RemissionEntity> dbRemissions = remissionRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<ApportionmentEntity> dbApportionments = apportionmentRepository.findByCcdCaseNumber(ccdCaseNumber);

        // Fetch refunds from refunds database
        List<String> paymentReferences = dbPayments.stream()
                .map(PaymentEntity::getReference)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<RefundEntity> dbRefunds = paymentReferences.isEmpty() ?
                List.of() : refundRepository.findByPaymentReferenceIn(paymentReferences);

        // Build indexes for lookup
        Map<Long, PaymentFeeLinkEntity> linkById = dbLinks.stream()
                .collect(Collectors.toMap(PaymentFeeLinkEntity::getId, l -> l));
        Map<String, PaymentFeeLinkEntity> linkByPaymentRef = dbLinks.stream()
                .filter(l -> l.getPaymentReference() != null)
                .collect(Collectors.toMap(PaymentFeeLinkEntity::getPaymentReference, l -> l, (a, b) -> a));
        Map<Long, FeeEntity> feeById = dbFees.stream()
                .collect(Collectors.toMap(FeeEntity::getId, f -> f));
        Map<Long, PaymentEntity> paymentById = dbPayments.stream()
                .collect(Collectors.toMap(PaymentEntity::getId, p -> p));
        Map<Long, ApportionmentEntity> apportionmentById = dbApportionments.stream()
                .collect(Collectors.toMap(ApportionmentEntity::getId, a -> a));
        Map<String, RemissionEntity> remissionByHwfRef = dbRemissions.stream()
                .filter(r -> r.getHwfReference() != null)
                .collect(Collectors.toMap(RemissionEntity::getHwfReference, r -> r, (a, b) -> a));

        // Track which entities to keep and their target payment_link_id
        Set<Long> serviceRequestIdsToKeep = new HashSet<>();
        Map<Long, Long> feeTargetLinkId = new HashMap<>();  // feeId -> target payment_link_id
        Map<Long, Long> paymentTargetLinkId = new HashMap<>();  // paymentId -> target payment_link_id
        Map<Long, Long> apportionmentTargetLinkId = new HashMap<>();  // apportionmentId -> target payment_link_id
        Map<String, Long> remissionTargetLinkId = new HashMap<>();  // hwfReference -> target payment_link_id
        Set<String> refundRefsToKeep = new HashSet<>();

        for (ServiceRequest sr : patchRequest.serviceRequests()) {
            // Find the service request by ID or payment reference
            PaymentFeeLinkEntity link = null;
            if (sr.id() != null) {
                link = linkById.get(sr.id());
            } else if (sr.paymentReference() != null) {
                link = linkByPaymentRef.get(sr.paymentReference());
            }

            if (link != null) {
                Long targetLinkId = link.getId();
                serviceRequestIdsToKeep.add(targetLinkId);

                // Process fees - track which link they should belong to
                for (Fee fee : sr.fees()) {
                    if (fee.id() != null) {
                        feeTargetLinkId.put(fee.id(), targetLinkId);
                    }
                    // Process remissions
                    for (Remission rem : fee.remissions()) {
                        if (rem.hwfReference() != null) {
                            remissionTargetLinkId.put(rem.hwfReference(), targetLinkId);
                        }
                    }
                }

                // Process payments - track which link they should belong to
                for (Payment payment : sr.payments()) {
                    Long paymentId = findPaymentId(dbPayments, payment, null);  // Allow cross-link matching
                    if (paymentId != null) {
                        paymentTargetLinkId.put(paymentId, targetLinkId);
                    }

                    // Process refunds
                    for (Refund refund : payment.refunds()) {
                        if (refund.reference() != null) {
                            refundRefsToKeep.add(refund.reference());
                        }
                    }

                    // Process apportionments - track which link they should belong to
                    for (Apportionment app : payment.apportionments()) {
                        if (app.id() != null) {
                            apportionmentTargetLinkId.put(app.id(), targetLinkId);
                        }
                    }
                }
            }
        }

        // Collect entities to delete (not in any keep set)
        List<PaymentFeeLinkEntity> linksToDelete = dbLinks.stream()
                .filter(l -> !serviceRequestIdsToKeep.contains(l.getId()))
                .toList();
        List<FeeEntity> feesToDelete = dbFees.stream()
                .filter(f -> !feeTargetLinkId.containsKey(f.getId()))
                .toList();
        List<PaymentEntity> paymentsToDelete = dbPayments.stream()
                .filter(p -> !paymentTargetLinkId.containsKey(p.getId()))
                .toList();
        List<RemissionEntity> remissionsToDelete = dbRemissions.stream()
                .filter(r -> !remissionTargetLinkId.containsKey(r.getHwfReference()))
                .toList();
        List<RefundEntity> refundsToDelete = dbRefunds.stream()
                .filter(r -> !refundRefsToKeep.contains(r.getReference()))
                .toList();
        List<ApportionmentEntity> apportionmentsToDelete = dbApportionments.stream()
                .filter(a -> !apportionmentTargetLinkId.containsKey(a.getId()))
                .toList();

        // Collect entities to move (in keep set but with different payment_link_id)
        List<FeeEntity> feesToMove = dbFees.stream()
                .filter(f -> feeTargetLinkId.containsKey(f.getId()))
                .filter(f -> !feeTargetLinkId.get(f.getId()).equals(f.getPaymentLinkId()))
                .toList();
        List<PaymentEntity> paymentsToMove = dbPayments.stream()
                .filter(p -> paymentTargetLinkId.containsKey(p.getId()))
                .filter(p -> !paymentTargetLinkId.get(p.getId()).equals(p.getPaymentLinkId()))
                .toList();
        List<RemissionEntity> remissionsToMove = dbRemissions.stream()
                .filter(r -> remissionTargetLinkId.containsKey(r.getHwfReference()))
                .filter(r -> !remissionTargetLinkId.get(r.getHwfReference()).equals(r.getPaymentLinkId()))
                .toList();
        List<ApportionmentEntity> apportionmentsToMove = dbApportionments.stream()
                .filter(a -> apportionmentTargetLinkId.containsKey(a.getId()))
                .filter(a -> !apportionmentTargetLinkId.get(a.getId()).equals(a.getPaymentLinkId()))
                .toList();

        // Generate SQL statements
        List<String> paymentDbSql = new ArrayList<>();
        List<String> refundsDbSql = new ArrayList<>();
        List<String> paymentDbRollbackSql = new ArrayList<>();
        List<String> refundsDbRollbackSql = new ArrayList<>();

        // 1. Generate UPDATE SQL for moves (do moves before deletes)
        // Move apportionments first (child entities)
        for (ApportionmentEntity app : apportionmentsToMove) {
            Long newLinkId = apportionmentTargetLinkId.get(app.getId());
            paymentDbSql.add(generateUpdateSql("fee_pay_apportion", "id", app.getId(),
                    "payment_link_id", newLinkId));
            paymentDbRollbackSql.add(generateUpdateSql("fee_pay_apportion", "id", app.getId(),
                    "payment_link_id", app.getPaymentLinkId()));
        }

        // Move remissions (depends on fee)
        for (RemissionEntity rem : remissionsToMove) {
            Long newLinkId = remissionTargetLinkId.get(rem.getHwfReference());
            paymentDbSql.add(generateUpdateSql("remission", "id", rem.getId(),
                    "payment_link_id", newLinkId));
            paymentDbRollbackSql.add(generateUpdateSql("remission", "id", rem.getId(),
                    "payment_link_id", rem.getPaymentLinkId()));
        }

        // Move fees
        for (FeeEntity fee : feesToMove) {
            Long newLinkId = feeTargetLinkId.get(fee.getId());
            paymentDbSql.add(generateUpdateSql("fee", "id", fee.getId(),
                    "payment_link_id", newLinkId));
            paymentDbRollbackSql.add(generateUpdateSql("fee", "id", fee.getId(),
                    "payment_link_id", fee.getPaymentLinkId()));
        }

        // Move payments
        for (PaymentEntity pay : paymentsToMove) {
            Long newLinkId = paymentTargetLinkId.get(pay.getId());
            paymentDbSql.add(generateUpdateSql("payment", "id", pay.getId(),
                    "payment_link_id", newLinkId));
            paymentDbRollbackSql.add(generateUpdateSql("payment", "id", pay.getId(),
                    "payment_link_id", pay.getPaymentLinkId()));
        }

        // 2. Generate DELETE SQL in dependency order (children first, then parents)
        // Delete apportionments (depends on fee and payment)
        for (ApportionmentEntity app : apportionmentsToDelete) {
            paymentDbSql.add(generateDeleteSql("fee_pay_apportion", "id", app.getId()));
        }

        // Delete remissions (depends on fee)
        for (RemissionEntity rem : remissionsToDelete) {
            paymentDbSql.add(generateDeleteSql("remission", "id", rem.getId()));
        }

        // Delete refunds (separate database)
        for (RefundEntity ref : refundsToDelete) {
            refundsDbSql.add(generateDeleteSql("refunds", "id", ref.getId()));
        }

        // Delete fees (depends on payment_fee_link)
        for (FeeEntity fee : feesToDelete) {
            paymentDbSql.add(generateDeleteSql("fee", "id", fee.getId()));
        }

        // Delete payments (depends on payment_fee_link)
        for (PaymentEntity pay : paymentsToDelete) {
            paymentDbSql.add(generateDeleteSql("payment", "id", pay.getId()));
        }

        // Delete payment_fee_links last
        for (PaymentFeeLinkEntity link : linksToDelete) {
            paymentDbSql.add(generateDeleteSql("payment_fee_link", "id", link.getId()));
        }

        // 3. Generate rollback INSERT SQL (parents first, children last) - only for deletes
        // Insert payment_fee_links first (parent)
        for (PaymentFeeLinkEntity link : linksToDelete) {
            paymentDbRollbackSql.add(generateInsertSql(link));
        }

        // Insert payments (depends on payment_fee_link)
        for (PaymentEntity pay : paymentsToDelete) {
            paymentDbRollbackSql.add(generateInsertSql(pay));
        }

        // Insert fees (depends on payment_fee_link)
        for (FeeEntity fee : feesToDelete) {
            paymentDbRollbackSql.add(generateInsertSql(fee));
        }

        // Insert refunds (separate database)
        for (RefundEntity ref : refundsToDelete) {
            refundsDbRollbackSql.add(generateInsertSql(ref));
        }

        // Insert remissions (depends on fee)
        for (RemissionEntity rem : remissionsToDelete) {
            paymentDbRollbackSql.add(generateInsertSql(rem));
        }

        // Insert apportionments last (depends on fee and payment)
        for (ApportionmentEntity app : apportionmentsToDelete) {
            paymentDbRollbackSql.add(generateInsertSql(app));
        }

        SqlGenerationResult.ChangeSummary summary = new SqlGenerationResult.ChangeSummary(
                linksToDelete.size(),
                feesToDelete.size(),
                paymentsToDelete.size(),
                remissionsToDelete.size(),
                refundsToDelete.size(),
                apportionmentsToDelete.size(),
                feesToMove.size(),
                paymentsToMove.size(),
                remissionsToMove.size(),
                apportionmentsToMove.size()
        );

        log.info("Generated {} payment DB SQL statements, {} refunds DB SQL statements, "
                + "{} payment DB rollback statements, {} refunds DB rollback statements. "
                + "Moves: {} fees, {} payments, {} remissions, {} apportionments",
                paymentDbSql.size(), refundsDbSql.size(),
                paymentDbRollbackSql.size(), refundsDbRollbackSql.size(),
                feesToMove.size(), paymentsToMove.size(), remissionsToMove.size(), apportionmentsToMove.size());

        return new SqlGenerationResult(paymentDbSql, refundsDbSql, paymentDbRollbackSql, refundsDbRollbackSql, summary);
    }

    private Long findPaymentId(List<PaymentEntity> dbPayments, Payment payment, Long paymentLinkId) {
        // First try by ID
        if (payment.id() != null) {
            return payment.id();
        }
        // Then try by reference (across all payment_links if paymentLinkId is null)
        if (payment.reference() != null) {
            return dbPayments.stream()
                    .filter(p -> payment.reference().equals(p.getReference()))
                    .filter(p -> paymentLinkId == null || paymentLinkId.equals(p.getPaymentLinkId()))
                    .map(PaymentEntity::getId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String generateDeleteSql(String tableName, String idColumn, Long id) {
        return String.format("DELETE FROM %s WHERE %s = %d;", tableName, idColumn, id);
    }

    private String generateDeleteSql(String tableName, String column, String value) {
        return String.format("DELETE FROM %s WHERE %s = '%s';", tableName, column, value.replace("'", "''"));
    }

    private String generateUpdateSql(String tableName, String idColumn, Long id, String updateColumn, Long newValue) {
        return String.format("UPDATE %s SET %s = %d WHERE %s = %d;",
                tableName, updateColumn, newValue, idColumn, id);
    }

    private String generateInsertSql(PaymentFeeLinkEntity entity) {
        return String.format(
            "INSERT INTO payment_fee_link (id, date_created, date_updated, payment_reference, org_id, "
            + "enterprise_service_name, ccd_case_number, case_reference, service_request_callback_url) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated()),
            formatString(entity.getPaymentReference()),
            formatString(entity.getOrgId()),
            formatString(entity.getEnterpriseServiceName()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getCaseReference()),
            formatString(entity.getServiceRequestCallbackUrl())
        );
    }

    private String generateInsertSql(FeeEntity entity) {
        return String.format(
            "INSERT INTO fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, "
            + "reference, net_amount, fee_amount, amount_due, date_created, date_updated) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatString(entity.getCode()),
            formatString(entity.getVersion()),
            formatLong(entity.getPaymentLinkId()),
            formatBigDecimal(entity.getCalculatedAmount()),
            formatInteger(entity.getVolume()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getReference()),
            formatBigDecimal(entity.getNetAmount()),
            formatBigDecimal(entity.getFeeAmount()),
            formatBigDecimal(entity.getAmountDue()),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated())
        );
    }

    private String generateInsertSql(PaymentEntity entity) {
        return String.format(
            "INSERT INTO payment (id, amount, case_reference, ccd_case_number, currency, date_created, date_updated, "
            + "description, service_type, site_id, user_id, payment_channel, payment_method, payment_provider, "
            + "payment_status, payment_link_id, customer_reference, external_reference, organisation_name, "
            + "pba_number, reference, giro_slip_no, s2s_service_name, reported_date_offline, service_callback_url, "
            + "document_control_number, banked_date, payer_name, internal_reference) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatBigDecimal(entity.getAmount()),
            formatString(entity.getCaseReference()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getCurrency()),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated()),
            formatString(entity.getDescription()),
            formatString(entity.getServiceType()),
            formatString(entity.getSiteId()),
            formatString(entity.getUserId()),
            formatString(entity.getPaymentChannel()),
            formatString(entity.getPaymentMethod()),
            formatString(entity.getPaymentProvider()),
            formatString(entity.getPaymentStatus()),
            formatLong(entity.getPaymentLinkId()),
            formatString(entity.getCustomerReference()),
            formatString(entity.getExternalReference()),
            formatString(entity.getOrganisationName()),
            formatString(entity.getPbaNumber()),
            formatString(entity.getReference()),
            formatString(entity.getGiroSlipNo()),
            formatString(entity.getS2sServiceName()),
            formatTimestamp(entity.getReportedDateOffline()),
            formatString(entity.getServiceCallbackUrl()),
            formatString(entity.getDocumentControlNumber()),
            formatTimestamp(entity.getBankedDate()),
            formatString(entity.getPayerName()),
            formatString(entity.getInternalReference())
        );
    }

    private String generateInsertSql(RemissionEntity entity) {
        return String.format(
            "INSERT INTO remission (id, fee_id, hwf_reference, hwf_amount, beneficiary_name, ccd_case_number, "
            + "case_reference, payment_link_id, site_id, date_created, date_updated, remission_reference) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatLong(entity.getFeeId()),
            formatString(entity.getHwfReference()),
            formatBigDecimal(entity.getHwfAmount()),
            formatString(entity.getBeneficiaryName()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getCaseReference()),
            formatLong(entity.getPaymentLinkId()),
            formatString(entity.getSiteId()),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated()),
            formatString(entity.getRemissionReference())
        );
    }

    private String generateInsertSql(ApportionmentEntity entity) {
        return String.format(
            "INSERT INTO fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, "
            + "apportion_amount, ccd_case_number, apportion_type, call_surplus_amount, created_by, date_created, date_updated) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatLong(entity.getPaymentId()),
            formatLong(entity.getFeeId()),
            formatLong(entity.getPaymentLinkId()),
            formatBigDecimal(entity.getFeeAmount()),
            formatBigDecimal(entity.getPaymentAmount()),
            formatBigDecimal(entity.getApportionAmount()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getApportionType()),
            formatBigDecimal(entity.getCallSurplusAmount()),
            formatString(entity.getCreatedBy()),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated())
        );
    }

    private String generateInsertSql(RefundEntity entity) {
        return String.format(
            "INSERT INTO refunds (id, date_created, date_updated, amount, reason, refund_status, reference, "
            + "payment_reference, created_by, updated_by, ccd_case_number, fee_ids, notification_sent_flag, "
            + "contact_details, service_type, refund_instruction_type) "
            + "VALUES (%d, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);",
            entity.getId(),
            formatTimestamp(entity.getDateCreated()),
            formatTimestamp(entity.getDateUpdated()),
            formatBigDecimal(entity.getAmount()),
            formatString(entity.getReason()),
            formatString(entity.getRefundStatus()),
            formatString(entity.getReference()),
            formatString(entity.getPaymentReference()),
            formatString(entity.getCreatedBy()),
            formatString(entity.getUpdatedBy()),
            formatString(entity.getCcdCaseNumber()),
            formatString(entity.getFeeIds()),
            formatString(entity.getNotificationSentFlag()),
            formatString(entity.getContactDetails()),
            formatString(entity.getServiceType()),
            formatString(entity.getRefundInstructionType())
        );
    }

    private String formatString(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private String formatLong(Long value) {
        return value == null ? "NULL" : value.toString();
    }

    private String formatInteger(Integer value) {
        return value == null ? "NULL" : value.toString();
    }

    private String formatBigDecimal(java.math.BigDecimal value) {
        return value == null ? "NULL" : value.toPlainString();
    }

    private String formatTimestamp(java.time.LocalDateTime value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.toString().replace("T", " ") + "'";
    }
}