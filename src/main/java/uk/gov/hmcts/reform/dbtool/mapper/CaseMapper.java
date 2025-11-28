package uk.gov.hmcts.reform.dbtool.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.domain.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps between database entities and domain models
 */
@Component
public class CaseMapper {

    /**
     * Map database entities to domain Case(s)
     */
    public List<Case> mapToDomain(
            List<PaymentFeeLinkEntity> links,
            List<FeeEntity> fees,
            List<PaymentEntity> payments,
            List<RefundEntity> refunds,
            List<RemissionEntity> remissions,
            List<ApportionmentEntity> apportionments) {

        // Index entities for efficient lookups
        Map<Long, List<FeeEntity>> feesByLinkId = fees.stream()
                .filter(f -> f.getPaymentLinkId() != null)
                .collect(Collectors.groupingBy(FeeEntity::getPaymentLinkId));

        Map<Long, List<PaymentEntity>> paymentsByLinkId = payments.stream()
                .filter(p -> p.getPaymentLinkId() != null)
                .collect(Collectors.groupingBy(PaymentEntity::getPaymentLinkId));

        Map<Long, List<RemissionEntity>> remissionsByFeeId = remissions.stream()
                .filter(r -> r.getFeeId() != null)
                .collect(Collectors.groupingBy(RemissionEntity::getFeeId));

        Map<String, List<RefundEntity>> refundsByPaymentRef = refunds.stream()
                .filter(r -> r.getPaymentReference() != null)
                .collect(Collectors.groupingBy(RefundEntity::getPaymentReference));

        Map<Long, List<ApportionmentEntity>> apportionmentsByPaymentId = apportionments.stream()
                .filter(a -> a.getPaymentId() != null)
                .collect(Collectors.groupingBy(ApportionmentEntity::getPaymentId));

        // Group links by CCD case number
        Map<String, List<PaymentFeeLinkEntity>> linksByCcd = links.stream()
                .filter(l -> l.getCcdCaseNumber() != null)
                .collect(Collectors.groupingBy(PaymentFeeLinkEntity::getCcdCaseNumber));

        // Build cases
        List<Case> result = new ArrayList<>();

        for (Map.Entry<String, List<PaymentFeeLinkEntity>> entry : linksByCcd.entrySet()) {
            String ccd = entry.getKey();
            List<PaymentFeeLinkEntity> caseLinks = entry.getValue();

            List<ServiceRequest> serviceRequests = caseLinks.stream()
                    .map(link -> buildServiceRequest(
                            link,
                            feesByLinkId.getOrDefault(link.getId(), List.of()),
                            paymentsByLinkId.getOrDefault(link.getId(), List.of()),
                            remissionsByFeeId,
                            refundsByPaymentRef,
                            apportionmentsByPaymentId))
                    .toList();

            Case domainCase = new Case(ccd);
            serviceRequests.forEach(domainCase::addServiceRequest);
            result.add(domainCase);
        }

        return result;
    }

    private ServiceRequest buildServiceRequest(
            PaymentFeeLinkEntity link,
            List<FeeEntity> linkFees,
            List<PaymentEntity> linkPayments,
            Map<Long, List<RemissionEntity>> remissionsByFeeId,
            Map<String, List<RefundEntity>> refundsByPaymentRef,
            Map<Long, List<ApportionmentEntity>> apportionmentsByPaymentId) {

        // Build fees with their remissions
        List<Fee> fees = linkFees.stream()
                .map(dbFee -> buildFee(dbFee, remissionsByFeeId.getOrDefault(dbFee.getId(), List.of())))
                .toList();

        // Build payments with their refunds and apportionments
        List<Payment> payments = linkPayments.stream()
                .map(dbPayment -> buildPayment(
                        dbPayment,
                        refundsByPaymentRef.getOrDefault(dbPayment.getReference(), List.of()),
                        apportionmentsByPaymentId.getOrDefault(dbPayment.getId(), List.of())))
                .toList();

        return new ServiceRequest(
                link.getId(),
                link.getPaymentReference(),
                link.getCcdCaseNumber(),
                link.getCaseReference(),
                fees,
                payments,
                link.getDateCreated(),
                link.getDateUpdated(),
                link.getOrgId(),
                link.getEnterpriseServiceName(),
                link.getServiceRequestCallbackUrl()
        );
    }

    private Fee buildFee(FeeEntity dbFee, List<RemissionEntity> feeRemissions) {
        List<Remission> remissions = feeRemissions.stream()
                .map(this::buildRemission)
                .toList();

        return new Fee(
                dbFee.getId(),
                dbFee.getCode(),
                dbFee.getVersion(),
                formatAmount(dbFee.getFeeAmount()),
                formatAmount(dbFee.getCalculatedAmount()),
                formatAmount(dbFee.getNetAmount()),
                formatAmount(dbFee.getAmountDue()),
                dbFee.getVolume(),
                dbFee.getReference(),
                remissions,
                dbFee.getDateCreated(),
                dbFee.getDateUpdated()
        );
    }

    private Remission buildRemission(RemissionEntity dbRemission) {
        return new Remission(
                dbRemission.getHwfReference(),
                dbRemission.getHwfAmount() != null ? dbRemission.getHwfAmount().doubleValue() : null,
                dbRemission.getBeneficiaryName(),
                dbRemission.getDateCreated(),
                dbRemission.getDateUpdated()
        );
    }

    private Payment buildPayment(
            PaymentEntity dbPayment,
            List<RefundEntity> paymentRefunds,
            List<ApportionmentEntity> paymentApportionments) {

        List<Refund> refunds = paymentRefunds.stream()
                .map(this::buildRefund)
                .toList();

        List<Apportionment> apportionments = paymentApportionments.stream()
                .map(this::buildApportionment)
                .toList();

        return new Payment(
                dbPayment.getId(),
                dbPayment.getReference(),
                formatAmount(dbPayment.getAmount()),
                dbPayment.getCurrency(),
                dbPayment.getPaymentStatus(),
                dbPayment.getPaymentMethod(),
                dbPayment.getPaymentProvider(),
                dbPayment.getPaymentChannel(),
                dbPayment.getExternalReference(),
                dbPayment.getCustomerReference(),
                dbPayment.getPbaNumber(),
                dbPayment.getPayerName(),
                dbPayment.getDateCreated(),
                dbPayment.getDateUpdated(),
                dbPayment.getBankedDate(),
                refunds,
                apportionments
        );
    }

    private Refund buildRefund(RefundEntity dbRefund) {
        return new Refund(
                dbRefund.getReference(),
                dbRefund.getAmount() != null ? dbRefund.getAmount().doubleValue() : null,
                dbRefund.getReason(),
                dbRefund.getRefundStatus(),
                dbRefund.getRefundInstructionType(),
                dbRefund.getDateCreated(),
                dbRefund.getDateUpdated(),
                dbRefund.getCreatedBy(),
                dbRefund.getUpdatedBy()
        );
    }

    private Apportionment buildApportionment(ApportionmentEntity dbApportion) {
        return new Apportionment(
                dbApportion.getId(),
                dbApportion.getFeeId(),
                formatAmount(dbApportion.getApportionAmount()),
                dbApportion.getApportionType(),
                formatAmount(dbApportion.getCallSurplusAmount()),
                dbApportion.getDateCreated(),
                dbApportion.getDateUpdated()
        );
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return null;
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}