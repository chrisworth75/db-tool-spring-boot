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
     * Query all data for a CCD case number from both databases
     */
    @Transactional(readOnly = true)
    public List<Case> queryCaseByCcd(String ccdCaseNumber) {
        log.info("Querying case data for CCD: {}", ccdCaseNumber);

        // Fetch all data directly by CCD in parallel
        List<PaymentFeeLinkEntity> links = paymentFeeLinkRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<FeeEntity> fees = feeRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<PaymentEntity> payments = paymentRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<RemissionEntity> remissions = remissionRepository.findByCcdCaseNumber(ccdCaseNumber);
        List<ApportionmentEntity> apportionments = apportionmentRepository.findByCcdCaseNumber(ccdCaseNumber);

        // Fetch refunds from refunds database
        List<String> paymentReferences = payments.stream()
                .map(PaymentEntity::getReference)
                .collect(Collectors.toList());
        List<RefundEntity> refunds = paymentReferences.isEmpty() ?
                List.of() : refundRepository.findByPaymentReferenceIn(paymentReferences);

        log.info("Found {} links, {} fees, {} payments, {} refunds, {} remissions, {} apportionments",
                links.size(), fees.size(), payments.size(), refunds.size(),
                remissions.size(), apportionments.size());

        // Map to domain model
        return caseMapper.mapToDomain(links, fees, payments, refunds, remissions, apportionments);
    }
}
