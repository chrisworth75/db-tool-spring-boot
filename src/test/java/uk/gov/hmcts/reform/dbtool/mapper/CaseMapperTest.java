package uk.gov.hmcts.reform.dbtool.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseMapperTest {

    private CaseMapper caseMapper;

    @BeforeEach
    void setUp() {
        caseMapper = new CaseMapper();
    }

    @Test
    void testMapToDomain_fullData() {
        // Setup
        String ccd = "1234567890123456";
        LocalDateTime now = LocalDateTime.now();

        PaymentFeeLinkEntity link = new PaymentFeeLinkEntity();
        link.setId(1L);
        link.setCcdCaseNumber(ccd);
        link.setPaymentReference("PAY-123");
        link.setCaseReference("CASE-123");
        link.setDateCreated(now);
        link.setDateUpdated(now);
        link.setOrgId("ORG-1");
        link.setEnterpriseServiceName("Service 1");
        link.setServiceRequestCallbackUrl("http://callback");

        FeeEntity fee = new FeeEntity();
        fee.setId(1L);
        fee.setCcdCaseNumber(ccd);
        fee.setPaymentLinkId(1L);
        fee.setCode("FEE001");
        fee.setVersion("1");
        fee.setFeeAmount(new BigDecimal("100.50"));
        fee.setCalculatedAmount(new BigDecimal("100.50"));
        fee.setNetAmount(new BigDecimal("100.50"));
        fee.setAmountDue(new BigDecimal("100.50"));
        fee.setVolume(1);
        fee.setReference("Fee ref");
        fee.setDateCreated(now);
        fee.setDateUpdated(now);

        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        payment.setCcdCaseNumber(ccd);
        payment.setPaymentLinkId(1L);
        payment.setReference("RC-123");
        payment.setAmount(new BigDecimal("100.50"));
        payment.setCurrency("GBP");
        payment.setPaymentStatus("Success");
        payment.setPaymentMethod("Card");
        payment.setPaymentProvider("Provider");
        payment.setPaymentChannel("Online");
        payment.setExternalReference("EXT-123");
        payment.setCustomerReference("CUST-123");
        payment.setPbaNumber("PBA123");
        payment.setPayerName("John Doe");
        payment.setDateCreated(now);
        payment.setDateUpdated(now);
        payment.setBankedDate(now);

        RemissionEntity remission = new RemissionEntity();
        remission.setId(1L);
        remission.setFeeId(1L);
        remission.setHwfReference("HWF-123");
        remission.setHwfAmount(new BigDecimal("50.25"));
        remission.setBeneficiaryName("Jane Doe");
        remission.setDateCreated(now);
        remission.setDateUpdated(now);

        RefundEntity refund = new RefundEntity();
        refund.setId(1L);
        refund.setPaymentReference("RC-123");
        refund.setReference("REF-123");
        refund.setAmount(new BigDecimal("25.00"));
        refund.setReason("Overpayment");
        refund.setRefundStatus("Approved");
        refund.setRefundInstructionType("AUTOMATED");
        refund.setDateCreated(now);
        refund.setDateUpdated(now);
        refund.setCreatedBy("system");
        refund.setUpdatedBy("admin");

        ApportionmentEntity apportionment = new ApportionmentEntity();
        apportionment.setId(1L);
        apportionment.setPaymentId(1L);
        apportionment.setFeeId(1L);
        apportionment.setApportionAmount(new BigDecimal("100.50"));
        apportionment.setApportionType("AUTO");
        apportionment.setCallSurplusAmount(new BigDecimal("0.00"));
        apportionment.setDateCreated(now);
        apportionment.setDateUpdated(now);

        // Execute
        List<Case> result = caseMapper.mapToDomain(
            List.of(link),
            List.of(fee),
            List.of(payment),
            List.of(refund),
            List.of(remission),
            List.of(apportionment)
        );

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());

        Case resultCase = result.get(0);
        assertEquals(ccd, resultCase.getCcdCaseNumber());
        assertEquals(1, resultCase.getServiceRequests().size());

        ServiceRequest sr = resultCase.getServiceRequests().get(0);
        assertEquals(1L, sr.id());
        assertEquals("PAY-123", sr.paymentReference());
        assertEquals(ccd, sr.ccdCaseNumber());
        assertEquals("CASE-123", sr.caseReference());
        assertEquals("ORG-1", sr.orgId());
        assertEquals("Service 1", sr.enterpriseServiceName());

        assertEquals(1, sr.fees().size());
        Fee resultFee = sr.fees().get(0);
        assertEquals(1L, resultFee.id());
        assertEquals("FEE001", resultFee.code());
        assertEquals("100.50", resultFee.amount());
        assertEquals(1, resultFee.remissions().size());

        Remission resultRemission = resultFee.remissions().get(0);
        assertEquals("HWF-123", resultRemission.hwfReference());
        assertEquals(50.25, resultRemission.amount());

        assertEquals(1, sr.payments().size());
        Payment resultPayment = sr.payments().get(0);
        assertEquals(1L, resultPayment.id());
        assertEquals("RC-123", resultPayment.reference());
        assertEquals("100.50", resultPayment.amount());
        assertEquals(1, resultPayment.refunds().size());
        assertEquals(1, resultPayment.apportionments().size());

        Refund resultRefund = resultPayment.refunds().get(0);
        assertEquals("REF-123", resultRefund.reference());
        assertEquals(25.0, resultRefund.amount());

        Apportionment resultApportionment = resultPayment.apportionments().get(0);
        assertEquals(1L, resultApportionment.id());
        assertEquals(1L, resultApportionment.feeId());
        assertEquals("100.50", resultApportionment.apportionAmount());
    }

    @Test
    void testMapToDomain_nullAmounts() {
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
        fee.setFeeAmount(null);
        fee.setCalculatedAmount(null);
        fee.setNetAmount(null);
        fee.setAmountDue(null);

        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        payment.setCcdCaseNumber(ccd);
        payment.setPaymentLinkId(1L);
        payment.setReference("RC-123");
        payment.setAmount(null);

        RemissionEntity remission = new RemissionEntity();
        remission.setId(1L);
        remission.setFeeId(1L);
        remission.setHwfAmount(null);

        RefundEntity refund = new RefundEntity();
        refund.setId(1L);
        refund.setPaymentReference("RC-123");
        refund.setAmount(null);

        ApportionmentEntity apportionment = new ApportionmentEntity();
        apportionment.setId(1L);
        apportionment.setPaymentId(1L);
        apportionment.setApportionAmount(null);
        apportionment.setCallSurplusAmount(null);

        // Execute
        List<Case> result = caseMapper.mapToDomain(
            List.of(link),
            List.of(fee),
            List.of(payment),
            List.of(refund),
            List.of(remission),
            List.of(apportionment)
        );

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());

        Case resultCase = result.get(0);
        ServiceRequest sr = resultCase.getServiceRequests().get(0);

        Fee resultFee = sr.fees().get(0);
        assertNull(resultFee.amount());
        assertNull(resultFee.calculatedAmount());

        Payment resultPayment = sr.payments().get(0);
        assertNull(resultPayment.amount());

        Remission resultRemission = resultFee.remissions().get(0);
        assertNull(resultRemission.amount());

        Refund resultRefund = resultPayment.refunds().get(0);
        assertNull(resultRefund.amount());
    }

    @Test
    void testMapToDomain_multipleCases() {
        // Setup
        String ccd1 = "1111111111111111";
        String ccd2 = "2222222222222222";

        PaymentFeeLinkEntity link1 = new PaymentFeeLinkEntity();
        link1.setId(1L);
        link1.setCcdCaseNumber(ccd1);
        link1.setPaymentReference("PAY-1");

        PaymentFeeLinkEntity link2 = new PaymentFeeLinkEntity();
        link2.setId(2L);
        link2.setCcdCaseNumber(ccd2);
        link2.setPaymentReference("PAY-2");

        FeeEntity fee1 = new FeeEntity();
        fee1.setId(1L);
        fee1.setCcdCaseNumber(ccd1);
        fee1.setPaymentLinkId(1L);

        FeeEntity fee2 = new FeeEntity();
        fee2.setId(2L);
        fee2.setCcdCaseNumber(ccd2);
        fee2.setPaymentLinkId(2L);

        PaymentEntity payment1 = new PaymentEntity();
        payment1.setId(1L);
        payment1.setCcdCaseNumber(ccd1);
        payment1.setPaymentLinkId(1L);
        payment1.setReference("RC-1");

        PaymentEntity payment2 = new PaymentEntity();
        payment2.setId(2L);
        payment2.setCcdCaseNumber(ccd2);
        payment2.setPaymentLinkId(2L);
        payment2.setReference("RC-2");

        // Execute
        List<Case> result = caseMapper.mapToDomain(
            List.of(link1, link2),
            List.of(fee1, fee2),
            List.of(payment1, payment2),
            List.of(),
            List.of(),
            List.of()
        );

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());

        Case case1 = result.stream()
            .filter(c -> c.getCcdCaseNumber().equals(ccd1))
            .findFirst()
            .orElse(null);
        assertNotNull(case1);
        assertEquals(1, case1.getServiceRequests().size());

        Case case2 = result.stream()
            .filter(c -> c.getCcdCaseNumber().equals(ccd2))
            .findFirst()
            .orElse(null);
        assertNotNull(case2);
        assertEquals(1, case2.getServiceRequests().size());
    }

    @Test
    void testMapToDomain_unmatchedEntities() {
        // Setup - fee and payment with non-existent link IDs
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = new PaymentFeeLinkEntity();
        link.setId(1L);
        link.setCcdCaseNumber(ccd);
        link.setPaymentReference("PAY-123");

        FeeEntity fee = new FeeEntity();
        fee.setId(1L);
        fee.setCcdCaseNumber(ccd);
        fee.setPaymentLinkId(999L); // Non-existent link

        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        payment.setCcdCaseNumber(ccd);
        payment.setPaymentLinkId(999L); // Non-existent link
        payment.setReference("RC-123");

        // Execute
        List<Case> result = caseMapper.mapToDomain(
            List.of(link),
            List.of(fee),
            List.of(payment),
            List.of(),
            List.of(),
            List.of()
        );

        // Verify - unmatched entities should be skipped
        assertNotNull(result);
        assertEquals(1, result.size());
        Case resultCase = result.get(0);
        ServiceRequest sr = resultCase.getServiceRequests().get(0);
        assertEquals(0, sr.fees().size());
        assertEquals(0, sr.payments().size());
    }
}