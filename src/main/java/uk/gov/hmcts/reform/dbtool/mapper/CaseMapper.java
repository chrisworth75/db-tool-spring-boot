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
            List<PaymentFeeLink> links,
            List<Fee> fees,
            List<Payment> payments,
            List<Refund> refunds,
            List<Remission> remissions,
            List<Apportionment> apportionments) {

        // Group by CCD case number
        Map<String, Case> caseMap = new HashMap<>();

        // Process payment_fee_links
        for (PaymentFeeLink link : links) {
            String ccd = link.getCcdCaseNumber();

            caseMap.putIfAbsent(ccd, new Case(ccd));
            Case domainCase = caseMap.get(ccd);

            ServiceRequest sr = new ServiceRequest(link.getPaymentReference(), ccd);
            sr.setCaseReference(link.getCaseReference());
            sr.setOrgId(link.getOrgId());
            sr.setServiceName(link.getEnterpriseServiceName());
            sr.setCreatedAt(link.getDateCreated());
            sr.setUpdatedAt(link.getDateUpdated());

            domainCase.addServiceRequest(sr);
        }

        // Add fees to service requests
        for (Fee dbFee : fees) {
            Case domainCase = caseMap.get(dbFee.getCcdCaseNumber());
            if (domainCase == null) continue;

            ServiceRequest sr = findServiceRequestByLinkId(domainCase, dbFee.getPaymentLinkId(), links);
            if (sr == null) continue;

            uk.gov.hmcts.reform.dbtool.domain.Fee fee = new uk.gov.hmcts.reform.dbtool.domain.Fee(
                    dbFee.getCode(),
                    dbFee.getVersion(),
                    dbFee.getFeeAmount() != null ? dbFee.getFeeAmount().doubleValue() : null
            );
            fee.setVolume(dbFee.getVolume());
            fee.setReference(dbFee.getReference());
            fee.setCreatedAt(dbFee.getDateCreated());
            fee.setUpdatedAt(dbFee.getDateUpdated());

            sr.addFee(fee);

            // Add remissions for this fee
            List<Remission> feeRemissions = remissions.stream()
                    .filter(r -> r.getFeeId().equals(dbFee.getId()))
                    .collect(Collectors.toList());

            for (Remission dbRemission : feeRemissions) {
                uk.gov.hmcts.reform.dbtool.domain.Remission remission =
                        new uk.gov.hmcts.reform.dbtool.domain.Remission(
                                dbRemission.getHwfReference(),
                                dbRemission.getHwfAmount() != null ? dbRemission.getHwfAmount().doubleValue() : null
                        );
                remission.setBeneficiaryName(dbRemission.getBeneficiaryName());
                remission.setCreatedAt(dbRemission.getDateCreated());
                remission.setUpdatedAt(dbRemission.getDateUpdated());

                fee.addRemission(remission);
            }
        }

        // Add payments to service requests
        for (Payment dbPayment : payments) {
            Case domainCase = caseMap.get(dbPayment.getCcdCaseNumber());
            if (domainCase == null) continue;

            ServiceRequest sr = findServiceRequestByLinkId(domainCase, dbPayment.getPaymentLinkId(), links);
            if (sr == null) continue;

            uk.gov.hmcts.reform.dbtool.domain.Payment payment =
                    new uk.gov.hmcts.reform.dbtool.domain.Payment(
                            dbPayment.getReference(),
                            dbPayment.getAmount() != null ? dbPayment.getAmount().doubleValue() : null
                    );
            payment.setCurrency(dbPayment.getCurrency());
            payment.setStatus(dbPayment.getPaymentStatus());
            payment.setMethod(dbPayment.getPaymentMethod());
            payment.setProvider(dbPayment.getPaymentProvider());
            payment.setChannel(dbPayment.getPaymentChannel());
            payment.setCustomerReference(dbPayment.getCustomerReference());
            payment.setPbaNumber(dbPayment.getPbaNumber());
            payment.setPayerName(dbPayment.getPayerName());
            payment.setCreatedAt(dbPayment.getDateCreated());
            payment.setUpdatedAt(dbPayment.getDateUpdated());
            payment.setBankedAt(dbPayment.getBankedDate());

            // Add fee allocations (apportionments) for this payment
            List<Apportionment> paymentApportionments = apportionments.stream()
                    .filter(a -> a.getPaymentId().equals(dbPayment.getId()))
                    .collect(Collectors.toList());

            for (Apportionment dbApportionment : paymentApportionments) {
                // Find the fee by id to get the fee code
                Optional<Fee> matchingFee = fees.stream()
                        .filter(f -> f.getId().equals(dbApportionment.getFeeId()))
                        .findFirst();

                matchingFee.ifPresent(fee -> payment.addFeeAllocation(
                        fee.getCode(),
                        dbApportionment.getApportionAmount() != null ?
                                dbApportionment.getApportionAmount().doubleValue() : null
                ));
            }

            sr.addPayment(payment);

            // Add refunds for this payment
            List<Refund> paymentRefunds = refunds.stream()
                    .filter(r -> dbPayment.getReference().equals(r.getPaymentReference()))
                    .collect(Collectors.toList());

            for (Refund dbRefund : paymentRefunds) {
                uk.gov.hmcts.reform.dbtool.domain.Refund refund =
                        new uk.gov.hmcts.reform.dbtool.domain.Refund(
                                dbRefund.getReference(),
                                dbRefund.getAmount() != null ? dbRefund.getAmount().doubleValue() : null,
                                dbRefund.getReason()
                        );
                refund.setStatus(dbRefund.getRefundStatus());
                refund.setInstructionType(dbRefund.getRefundInstructionType());
                refund.setCreatedAt(dbRefund.getDateCreated());
                refund.setUpdatedAt(dbRefund.getDateUpdated());
                refund.setCreatedBy(dbRefund.getCreatedBy());
                refund.setUpdatedBy(dbRefund.getUpdatedBy());

                payment.addRefund(refund);
            }
        }

        return new ArrayList<>(caseMap.values());
    }

    private ServiceRequest findServiceRequestByLinkId(
            Case domainCase, Long linkId, List<PaymentFeeLink> links) {

        Optional<PaymentFeeLink> link = links.stream()
                .filter(l -> l.getId().equals(linkId))
                .findFirst();

        if (link.isEmpty()) return null;

        return domainCase.getServiceRequests().stream()
                .filter(sr -> sr.getPaymentReference().equals(link.get().getPaymentReference()))
                .findFirst()
                .orElse(null);
    }
}
