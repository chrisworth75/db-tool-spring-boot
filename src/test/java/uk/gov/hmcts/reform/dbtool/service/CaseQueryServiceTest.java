package uk.gov.hmcts.reform.dbtool.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.mapper.CaseMapper;
import uk.gov.hmcts.reform.dbtool.repository.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseQueryServiceTest {

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RemissionRepository remissionRepository;

    @Mock
    private ApportionmentRepository apportionmentRepository;

    @Mock
    private CaseMapper caseMapper;

    @InjectMocks
    private CaseQueryService caseQueryService;

    @Test
    void testQueryCaseByCcd_withData() {
        // Setup
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = new PaymentFeeLinkEntity();
        link.setId(1L);
        link.setCcdCaseNumber(ccd);
        link.setPaymentReference("PAY-123");

        FeeEntity fee = new FeeEntity();
        fee.setId(1L);
        fee.setCcdCaseNumber(ccd);
        fee.setPaymentLinkId(1L);
        fee.setFeeAmount(new BigDecimal("100.00"));

        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        payment.setCcdCaseNumber(ccd);
        payment.setReference("RC-123");
        payment.setAmount(new BigDecimal("100.00"));

        RemissionEntity remission = new RemissionEntity();
        remission.setId(1L);
        remission.setCcdCaseNumber(ccd);

        ApportionmentEntity apportionment = new ApportionmentEntity();
        apportionment.setId(1L);
        apportionment.setCcdCaseNumber(ccd);

        RefundEntity refund = new RefundEntity();
        refund.setId(1L);
        refund.setPaymentReference("RC-123");

        Case mockCase = new Case(ccd);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(payment));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(remission));
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(apportionment));
        when(refundRepository.findByPaymentReferenceIn(any())).thenReturn(List.of(refund));
        when(caseMapper.mapToDomain(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(mockCase));

        // Execute
        List<Case> result = caseQueryService.queryCaseByCcd(ccd);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ccd, result.get(0).getCcdCaseNumber());

        verify(paymentFeeLinkRepository).findByCcdCaseNumber(ccd);
        verify(feeRepository).findByCcdCaseNumber(ccd);
        verify(paymentRepository).findByCcdCaseNumber(ccd);
        verify(remissionRepository).findByCcdCaseNumber(ccd);
        verify(apportionmentRepository).findByCcdCaseNumber(ccd);
        verify(refundRepository).findByPaymentReferenceIn(List.of("RC-123"));
        verify(caseMapper).mapToDomain(
            List.of(link),
            List.of(fee),
            List.of(payment),
            List.of(refund),
            List.of(remission),
            List.of(apportionment)
        );
    }

    @Test
    void testQueryCaseByCcd_noPayments_noRefundQuery() {
        // Setup
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = new PaymentFeeLinkEntity();
        link.setId(1L);
        link.setCcdCaseNumber(ccd);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(caseMapper.mapToDomain(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(new Case(ccd)));

        // Execute
        List<Case> result = caseQueryService.queryCaseByCcd(ccd);

        // Verify
        assertNotNull(result);
        verify(refundRepository, never()).findByPaymentReferenceIn(any());
    }

    @Test
    void testQueryCaseByCcd_multipleSources() {
        // Setup
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = new PaymentFeeLinkEntity();
        link1.setId(1L);
        link1.setCcdCaseNumber(ccd);

        PaymentFeeLinkEntity link2 = new PaymentFeeLinkEntity();
        link2.setId(2L);
        link2.setCcdCaseNumber(ccd);

        FeeEntity fee1 = new FeeEntity();
        fee1.setId(1L);
        fee1.setCcdCaseNumber(ccd);

        FeeEntity fee2 = new FeeEntity();
        fee2.setId(2L);
        fee2.setCcdCaseNumber(ccd);

        PaymentEntity payment1 = new PaymentEntity();
        payment1.setId(1L);
        payment1.setReference("RC-1");

        PaymentEntity payment2 = new PaymentEntity();
        payment2.setId(2L);
        payment2.setReference("RC-2");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd))
            .thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd))
            .thenReturn(List.of(fee1, fee2));
        when(paymentRepository.findByCcdCaseNumber(ccd))
            .thenReturn(List.of(payment1, payment2));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(refundRepository.findByPaymentReferenceIn(any())).thenReturn(List.of());
        when(caseMapper.mapToDomain(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(new Case(ccd), new Case(ccd)));

        // Execute
        List<Case> result = caseQueryService.queryCaseByCcd(ccd);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(refundRepository).findByPaymentReferenceIn(List.of("RC-1", "RC-2"));
    }
}
