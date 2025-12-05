package uk.gov.hmcts.reform.dbtool.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.dbtool.database.*;
import uk.gov.hmcts.reform.dbtool.domain.*;
import uk.gov.hmcts.reform.dbtool.repository.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseDiffServiceTest {

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RemissionRepository remissionRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ApportionmentRepository apportionmentRepository;

    @InjectMocks
    private CaseDiffService caseDiffService;

    @Test
    void testGenerateDeletionSql_deleteOneOfTwoServiceRequests() {
        String ccd = "1234567890123456";

        // Database has two service requests
        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Patch request keeps only the first one (by ID)
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(1L, null, List.of(), List.of()))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().serviceRequestsToDelete());
        assertEquals(1, result.paymentDatabaseSql().size());
        assertTrue(result.paymentDatabaseSql().get(0).contains("DELETE FROM payment_fee_link WHERE id = 2"));
    }

    @Test
    void testGenerateDeletionSql_deleteByPaymentReference() {
        String ccd = "1234567890123456";

        // Database has two service requests
        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Patch request keeps only the first one (by payment reference)
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(null, "PAY-001", List.of(), List.of()))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().serviceRequestsToDelete());
        assertTrue(result.paymentDatabaseSql().get(0).contains("DELETE FROM payment_fee_link WHERE id = 2"));
    }

    @Test
    void testGenerateDeletionSql_deleteFeeFromServiceRequest() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee1 = createFeeEntity(101L, ccd, 1L);
        FeeEntity fee2 = createFeeEntity(102L, ccd, 1L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee1, fee2));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep service request but only one fee
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of())),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(0, result.summary().serviceRequestsToDelete());
        assertEquals(1, result.summary().feesToDelete());
        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("DELETE FROM fee WHERE id = 102")));
    }

    @Test
    void testGenerateDeletionSql_deletePaymentWithCascade() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        PaymentEntity payment1 = createPaymentEntity(201L, ccd, 1L, "RC-001");
        PaymentEntity payment2 = createPaymentEntity(202L, ccd, 1L, "RC-002");
        ApportionmentEntity apportion = createApportionmentEntity(301L, ccd, 202L);
        RefundEntity refund = createRefundEntity(401L, "RF-001", "RC-002");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(payment1, payment2));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(apportion));
        when(refundRepository.findByPaymentReferenceIn(any())).thenReturn(List.of(refund));

        // Keep only payment1
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(),
                List.of(createPayment(201L, "RC-001", List.of(), List.of()))
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().paymentsToDelete());
        assertEquals(1, result.summary().apportionmentsToDelete());
        assertEquals(1, result.summary().refundsToDelete());

        // Verify deletion order: apportionments first, then payments
        List<String> sql = result.paymentDatabaseSql();
        int apportionmentIdx = -1;
        int paymentIdx = -1;
        for (int i = 0; i < sql.size(); i++) {
            if (sql.get(i).contains("fee_pay_apportion")) apportionmentIdx = i;
            if (sql.get(i).contains("DELETE FROM payment WHERE")) paymentIdx = i;
        }
        assertTrue(apportionmentIdx < paymentIdx, "Apportionments should be deleted before payments");

        // Refund should be in separate list
        assertEquals(1, result.refundsDatabaseSql().size());
        assertTrue(result.refundsDatabaseSql().get(0).contains("DELETE FROM refunds"));
    }

    @Test
    void testGenerateDeletionSql_deleteRemission() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee = createFeeEntity(101L, ccd, 1L);
        RemissionEntity rem1 = createRemissionEntity(501L, ccd, 101L, "HWF-001");
        RemissionEntity rem2 = createRemissionEntity(502L, ccd, 101L, "HWF-002");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(rem1, rem2));
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep fee with only one remission
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of(createRemission("HWF-001")))),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().remissionsToDelete());
        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("DELETE FROM remission WHERE id = 502")));
    }

    @Test
    void testGenerateDeletionSql_noChanges() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee = createFeeEntity(101L, ccd, 1L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep everything
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of())),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(0, result.summary().serviceRequestsToDelete());
        assertEquals(0, result.summary().feesToDelete());
        assertTrue(result.paymentDatabaseSql().isEmpty());
        assertTrue(result.refundsDatabaseSql().isEmpty());
    }

    @Test
    void testGenerateDeletionSql_emptyPatchDeletesAll() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee = createFeeEntity(101L, ccd, 1L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Empty patch - delete everything
        CasePatchRequest request = new CasePatchRequest(ccd, List.of());

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().serviceRequestsToDelete());
        assertEquals(1, result.summary().feesToDelete());
    }

    // Helper methods to create domain DTOs for patch requests
    private ServiceRequest createServiceRequest(Long id, String paymentReference, List<Fee> fees, List<Payment> payments) {
        return new ServiceRequest(id, paymentReference, null, null, fees, payments, null, null, null, null, null);
    }

    private Fee createFee(Long id, List<Remission> remissions) {
        return new Fee(id, null, null, null, null, null, null, null, null, remissions, null, null);
    }

    private Payment createPayment(Long id, String reference, List<Refund> refunds, List<Apportionment> apportionments) {
        return new Payment(id, reference, null, null, null, null, null, null, null, null, null, null, null, null, null, refunds, apportionments);
    }

    private Remission createRemission(String hwfReference) {
        return new Remission(hwfReference, null, null, null, null);
    }

    private Refund createRefund(String reference) {
        return new Refund(reference, null, null, null, null, null, null, null, null);
    }

    private Apportionment createApportionment(Long id) {
        return new Apportionment(id, null, null, null, null, null, null);
    }

    // Helper methods to create database entities
    private PaymentFeeLinkEntity createLink(Long id, String ccd, String paymentRef) {
        PaymentFeeLinkEntity link = new PaymentFeeLinkEntity();
        link.setId(id);
        link.setCcdCaseNumber(ccd);
        link.setPaymentReference(paymentRef);
        return link;
    }

    private FeeEntity createFeeEntity(Long id, String ccd, Long paymentLinkId) {
        FeeEntity fee = new FeeEntity();
        fee.setId(id);
        fee.setCcdCaseNumber(ccd);
        fee.setPaymentLinkId(paymentLinkId);
        fee.setFeeAmount(new BigDecimal("100.00"));
        return fee;
    }

    private PaymentEntity createPaymentEntity(Long id, String ccd, Long paymentLinkId, String reference) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(id);
        payment.setCcdCaseNumber(ccd);
        payment.setPaymentLinkId(paymentLinkId);
        payment.setReference(reference);
        payment.setAmount(new BigDecimal("100.00"));
        return payment;
    }

    private ApportionmentEntity createApportionmentEntity(Long id, String ccd, Long paymentId) {
        ApportionmentEntity app = new ApportionmentEntity();
        app.setId(id);
        app.setCcdCaseNumber(ccd);
        app.setPaymentId(paymentId);
        return app;
    }

    private RemissionEntity createRemissionEntity(Long id, String ccd, Long feeId, String hwfRef) {
        RemissionEntity rem = new RemissionEntity();
        rem.setId(id);
        rem.setCcdCaseNumber(ccd);
        rem.setFeeId(feeId);
        rem.setHwfReference(hwfRef);
        return rem;
    }

    private RefundEntity createRefundEntity(Long id, String reference, String paymentReference) {
        RefundEntity refund = new RefundEntity();
        refund.setId(id);
        refund.setReference(reference);
        refund.setPaymentReference(paymentReference);
        return refund;
    }
}