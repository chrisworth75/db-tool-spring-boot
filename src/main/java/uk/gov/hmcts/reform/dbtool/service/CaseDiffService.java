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
 * and generating SQL deletion statements.
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
     * Compare the patch request with the database state and generate SQL for deletions.
     * Items present in the patch request are kept; absent items are deleted.
     */
    @Transactional(readOnly = true)
    public SqlGenerationResult generateDeletionSql(CasePatchRequest patchRequest) {
        String ccdCaseNumber = patchRequest.ccdCaseNumber();
        log.info("Generating deletion SQL for CCD: {}", ccdCaseNumber);

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

        // Determine which entities to keep based on patch request
        Set<Long> serviceRequestIdsToKeep = new HashSet<>();
        Set<Long> feeIdsToKeep = new HashSet<>();
        Set<Long> paymentIdsToKeep = new HashSet<>();
        Set<String> remissionHwfRefsToKeep = new HashSet<>();
        Set<String> refundRefsToKeep = new HashSet<>();
        Set<Long> apportionmentIdsToKeep = new HashSet<>();

        for (ServiceRequestPatch srPatch : patchRequest.serviceRequests()) {
            // Find the service request by ID or payment reference
            PaymentFeeLinkEntity link = null;
            if (srPatch.id() != null) {
                link = linkById.get(srPatch.id());
            } else if (srPatch.paymentReference() != null) {
                link = linkByPaymentRef.get(srPatch.paymentReference());
            }

            if (link != null) {
                serviceRequestIdsToKeep.add(link.getId());

                // Process fees to keep
                for (FeePatch feePatch : srPatch.fees()) {
                    if (feePatch.id() != null) {
                        feeIdsToKeep.add(feePatch.id());
                    }
                    // Process remissions to keep
                    for (RemissionPatch remPatch : feePatch.remissions()) {
                        if (remPatch.hwfReference() != null) {
                            remissionHwfRefsToKeep.add(remPatch.hwfReference());
                        }
                    }
                }

                // Process payments to keep
                for (PaymentPatch payPatch : srPatch.payments()) {
                    Long paymentId = findPaymentId(dbPayments, payPatch, link.getId());
                    if (paymentId != null) {
                        paymentIdsToKeep.add(paymentId);
                    }

                    // Process refunds to keep
                    for (RefundPatch refPatch : payPatch.refunds()) {
                        if (refPatch.reference() != null) {
                            refundRefsToKeep.add(refPatch.reference());
                        }
                    }

                    // Process apportionments to keep
                    for (ApportionmentPatch appPatch : payPatch.apportionments()) {
                        if (appPatch.id() != null) {
                            apportionmentIdsToKeep.add(appPatch.id());
                        }
                    }
                }
            }
        }

        // Generate SQL statements
        List<String> paymentDbSql = new ArrayList<>();
        List<String> refundsDbSql = new ArrayList<>();

        // Collect entities to delete
        List<PaymentFeeLinkEntity> linksToDelete = dbLinks.stream()
                .filter(l -> !serviceRequestIdsToKeep.contains(l.getId()))
                .toList();
        List<FeeEntity> feesToDelete = dbFees.stream()
                .filter(f -> !feeIdsToKeep.contains(f.getId()))
                .toList();
        List<PaymentEntity> paymentsToDelete = dbPayments.stream()
                .filter(p -> !paymentIdsToKeep.contains(p.getId()))
                .toList();
        List<RemissionEntity> remissionsToDelete = dbRemissions.stream()
                .filter(r -> !remissionHwfRefsToKeep.contains(r.getHwfReference()))
                .toList();
        List<RefundEntity> refundsToDelete = dbRefunds.stream()
                .filter(r -> !refundRefsToKeep.contains(r.getReference()))
                .toList();
        List<ApportionmentEntity> apportionmentsToDelete = dbApportionments.stream()
                .filter(a -> !apportionmentIdsToKeep.contains(a.getId()))
                .toList();

        // Generate SQL in dependency order (children first, then parents)
        // 1. Delete apportionments (depends on fee and payment)
        for (ApportionmentEntity app : apportionmentsToDelete) {
            paymentDbSql.add(generateDeleteSql("fee_pay_apportion", "id", app.getId()));
        }

        // 2. Delete remissions (depends on fee)
        for (RemissionEntity rem : remissionsToDelete) {
            paymentDbSql.add(generateDeleteSql("remission", "id", rem.getId()));
        }

        // 3. Delete refunds (separate database, depends on payment reference)
        for (RefundEntity ref : refundsToDelete) {
            refundsDbSql.add(generateDeleteSql("refunds", "id", ref.getId()));
        }

        // 4. Delete fees (depends on payment_fee_link)
        for (FeeEntity fee : feesToDelete) {
            paymentDbSql.add(generateDeleteSql("fee", "id", fee.getId()));
        }

        // 5. Delete payments (depends on payment_fee_link)
        for (PaymentEntity pay : paymentsToDelete) {
            paymentDbSql.add(generateDeleteSql("payment", "id", pay.getId()));
        }

        // 6. Delete payment_fee_links last
        for (PaymentFeeLinkEntity link : linksToDelete) {
            paymentDbSql.add(generateDeleteSql("payment_fee_link", "id", link.getId()));
        }

        SqlGenerationResult.DeletionSummary summary = new SqlGenerationResult.DeletionSummary(
                linksToDelete.size(),
                feesToDelete.size(),
                paymentsToDelete.size(),
                remissionsToDelete.size(),
                refundsToDelete.size(),
                apportionmentsToDelete.size()
        );

        log.info("Generated {} payment DB SQL statements and {} refunds DB SQL statements",
                paymentDbSql.size(), refundsDbSql.size());

        return new SqlGenerationResult(paymentDbSql, refundsDbSql, summary);
    }

    private Long findPaymentId(List<PaymentEntity> dbPayments, PaymentPatch payPatch, Long paymentLinkId) {
        // First try by ID
        if (payPatch.id() != null) {
            return payPatch.id();
        }
        // Then try by reference within the same payment_link
        if (payPatch.reference() != null) {
            return dbPayments.stream()
                    .filter(p -> payPatch.reference().equals(p.getReference()))
                    .filter(p -> paymentLinkId.equals(p.getPaymentLinkId()))
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
}