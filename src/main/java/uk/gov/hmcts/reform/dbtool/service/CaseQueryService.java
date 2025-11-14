package uk.gov.hmcts.reform.dbtool.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.mapper.CaseMapper;
import uk.gov.hmcts.reform.dbtool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying case data from both databases
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseQueryService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RemissionRepository remissionRepository;
    private final ApportionmentRepository apportionmentRepository;
    private final CaseMapper caseMapper;

    /**
     * Query all data for a CCD case number
     */
    @Transactional(readOnly = true)
    public List<Case> queryCaseByCcd(String ccdCaseNumber) {
        log.info("Querying case data for CCD: {}", ccdCaseNumber);

        // Fetch payment_fee_links
        List<PaymentFeeLink> links = paymentFeeLinkRepository.findByCcdCaseNumber(ccdCaseNumber);
        if (links.isEmpty()) {
            log.warn("No payment_fee_links found for CCD: {}", ccdCaseNumber);
            return List.of();
        }

        List<Long> linkIds = links.stream()
                .map(PaymentFeeLink::getId)
                .collect(Collectors.toList());

        // Fetch all related data in parallel
        List<Fee> fees = feeRepository.findByPaymentLinkIdIn(linkIds);
        List<Payment> payments = paymentRepository.findByPaymentLinkIdIn(linkIds);
        List<Remission> remissions = remissionRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<Apportionment> apportionments = apportionmentRepository.findByCcdCaseNumber(ccdCaseNumber);

        // Fetch refunds
        List<String> paymentReferences = payments.stream()
                .map(Payment::getReference)
                .collect(Collectors.toList());
        List<Refund> refunds = paymentReferences.isEmpty() ?
                List.of() : refundRepository.findByPaymentReferenceIn(paymentReferences);

        log.info("Found {} links, {} fees, {} payments, {} refunds, {} remissions, {} apportionments",
                links.size(), fees.size(), payments.size(), refunds.size(),
                remissions.size(), apportionments.size());

        // Map to domain model
        return caseMapper.mapToDomain(links, fees, payments, refunds, remissions, apportionments);
    }

    /**
     * Query all data for a payment reference
     */
    @Transactional(readOnly = true)
    public List<Case> queryCaseByPaymentReference(String paymentReference) {
        log.info("Querying case data for payment reference: {}", paymentReference);

        // Find the payment_fee_link first
        List<PaymentFeeLink> links = paymentFeeLinkRepository.findByPaymentReference(paymentReference);
        if (links.isEmpty()) {
            log.warn("No payment_fee_link found for payment reference: {}", paymentReference);
            return List.of();
        }

        // Get the CCD from the first link and query by CCD
        String ccdCaseNumber = links.get(0).getCcdCaseNumber();
        return queryCaseByCcd(ccdCaseNumber);
    }
}
