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

    @Test
    void testGenerateDeletionSql_rollbackSqlGenerated() {
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

        // Should have rollback SQL for each deleted entity
        assertEquals(2, result.paymentDatabaseRollbackSql().size());
        assertTrue(result.paymentDatabaseRollbackSql().stream()
            .anyMatch(sql -> sql.contains("INSERT INTO payment_fee_link")));
        assertTrue(result.paymentDatabaseRollbackSql().stream()
            .anyMatch(sql -> sql.contains("INSERT INTO fee")));
    }

    @Test
    void testGenerateDeletionSql_rollbackSqlInCorrectOrder() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee = createFeeEntity(101L, ccd, 1L);
        PaymentEntity payment = createPaymentEntity(201L, ccd, 1L, "RC-001");
        ApportionmentEntity apportion = createApportionmentEntity(301L, ccd, 201L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(payment));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(apportion));

        CasePatchRequest request = new CasePatchRequest(ccd, List.of());

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        // Verify rollback order: parents first, children last
        List<String> rollbackSql = result.paymentDatabaseRollbackSql();

        int linkIdx = -1;
        int paymentIdx = -1;
        int feeIdx = -1;
        int apportionmentIdx = -1;

        for (int i = 0; i < rollbackSql.size(); i++) {
            if (rollbackSql.get(i).contains("INSERT INTO payment_fee_link")) linkIdx = i;
            if (rollbackSql.get(i).contains("INSERT INTO payment (")) paymentIdx = i;
            if (rollbackSql.get(i).contains("INSERT INTO fee (")) feeIdx = i;
            if (rollbackSql.get(i).contains("INSERT INTO fee_pay_apportion")) apportionmentIdx = i;
        }

        // Parents should be inserted first
        assertTrue(linkIdx < paymentIdx, "payment_fee_link should be inserted before payment");
        assertTrue(linkIdx < feeIdx, "payment_fee_link should be inserted before fee");
        assertTrue(paymentIdx < apportionmentIdx, "payment should be inserted before apportionment");
        assertTrue(feeIdx < apportionmentIdx, "fee should be inserted before apportionment");
    }

    @Test
    void testGenerateDeletionSql_rollbackContainsCorrectData() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        link.setOrgId("ORG123");
        link.setEnterpriseServiceName("TestService");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        CasePatchRequest request = new CasePatchRequest(ccd, List.of());

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        String insertSql = result.paymentDatabaseRollbackSql().get(0);
        assertTrue(insertSql.contains("'PAY-001'"), "Should contain payment reference");
        assertTrue(insertSql.contains("'ORG123'"), "Should contain org_id");
        assertTrue(insertSql.contains("'TestService'"), "Should contain enterprise_service_name");
        assertTrue(insertSql.contains("'" + ccd + "'"), "Should contain ccd_case_number");
    }

    @Test
    void testGenerateDeletionSql_rollbackSqlForRefundsInSeparateList() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        PaymentEntity payment = createPaymentEntity(201L, ccd, 1L, "RC-001");
        RefundEntity refund = createRefundEntity(401L, "RF-001", "RC-001");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(payment));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(refundRepository.findByPaymentReferenceIn(any())).thenReturn(List.of(refund));

        CasePatchRequest request = new CasePatchRequest(ccd, List.of());

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        // Refund rollback should be in separate list
        assertEquals(1, result.refundsDatabaseRollbackSql().size());
        assertTrue(result.refundsDatabaseRollbackSql().get(0).contains("INSERT INTO refunds"));
        assertTrue(result.refundsDatabaseRollbackSql().get(0).contains("'RF-001'"));

        // Payment DB rollback should NOT contain refunds
        assertFalse(result.paymentDatabaseRollbackSql().stream()
            .anyMatch(sql -> sql.contains("INSERT INTO refunds")));
    }

    @Test
    void testGenerateDeletionSql_noRollbackSqlWhenNoChanges() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep everything
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(1L, null, List.of(), List.of()))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertTrue(result.paymentDatabaseRollbackSql().isEmpty());
        assertTrue(result.refundsDatabaseRollbackSql().isEmpty());
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

    // ==================== MOVE FUNCTIONALITY TESTS ====================

    @Test
    void testGenerateDeletionSql_moveFeeToAnotherServiceRequest() {
        String ccd = "1234567890123456";

        // Database has two service requests
        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Fee is currently on link2, but we want to move it to link1
        FeeEntity fee = createFeeEntity(101L, ccd, 2L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep link1 with the fee that's currently on link2
        // Delete link2 (since we're not keeping it)
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of())),  // Fee to move to link1
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        // Should move fee from link2 to link1
        assertEquals(1, result.summary().feesToMove());
        assertEquals(0, result.summary().feesToDelete());
        assertEquals(1, result.summary().serviceRequestsToDelete()); // link2 should be deleted

        // Verify UPDATE SQL is generated
        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE fee SET payment_link_id = 1 WHERE id = 101")));

        // Verify rollback UPDATE SQL reverses the move
        assertTrue(result.paymentDatabaseRollbackSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE fee SET payment_link_id = 2 WHERE id = 101")));
    }

    @Test
    void testGenerateDeletionSql_movePaymentToAnotherServiceRequest() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Payment is currently on link2
        PaymentEntity payment = createPaymentEntity(201L, ccd, 2L, "RC-001");

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(payment));
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep link1 with the payment that's currently on link2
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(),
                List.of(createPayment(201L, "RC-001", List.of(), List.of()))
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().paymentsToMove());
        assertEquals(0, result.summary().paymentsToDelete());

        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE payment SET payment_link_id = 1 WHERE id = 201")));
    }

    @Test
    void testGenerateDeletionSql_moveApportionmentToAnotherServiceRequest() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Apportionment is currently on link2
        ApportionmentEntity apportion = createApportionmentEntity(301L, ccd, 201L);
        apportion.setPaymentLinkId(2L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(apportion));

        // Keep link1 with the apportionment that's currently on link2
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(),
                List.of(createPayment(null, null, List.of(), List.of(createApportionment(301L))))
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().apportionmentsToMove());
        assertEquals(0, result.summary().apportionmentsToDelete());

        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE fee_pay_apportion SET payment_link_id = 1 WHERE id = 301")));
    }

    @Test
    void testGenerateDeletionSql_moveRemissionToAnotherServiceRequest() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Remission is currently on link2
        RemissionEntity remission = createRemissionEntity(501L, ccd, 101L, "HWF-001");
        remission.setPaymentLinkId(2L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(remission));
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep link1 with the remission that's currently on link2
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(null, List.of(createRemission("HWF-001")))),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(1, result.summary().remissionsToMove());
        assertEquals(0, result.summary().remissionsToDelete());

        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE remission SET payment_link_id = 1 WHERE id = 501")));
    }

    @Test
    void testGenerateDeletionSql_updateSqlBeforeDeleteSql() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Fee on link2 to move, and link2 to delete
        FeeEntity fee = createFeeEntity(101L, ccd, 2L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of())),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        List<String> sql = result.paymentDatabaseSql();
        int updateIdx = -1;
        int deleteIdx = -1;

        for (int i = 0; i < sql.size(); i++) {
            if (sql.get(i).contains("UPDATE fee SET payment_link_id")) updateIdx = i;
            if (sql.get(i).contains("DELETE FROM payment_fee_link")) deleteIdx = i;
        }

        assertTrue(updateIdx >= 0, "Should have UPDATE statement");
        assertTrue(deleteIdx >= 0, "Should have DELETE statement");
        assertTrue(updateIdx < deleteIdx, "UPDATE should come before DELETE");
    }

    @Test
    void testGenerateDeletionSql_combinedMoveAndDelete() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link1 = createLink(1L, ccd, "PAY-001");
        PaymentFeeLinkEntity link2 = createLink(2L, ccd, "PAY-002");

        // Fee1 on link1 - keep
        // Fee2 on link2 - move to link1
        // Fee3 on link2 - delete
        FeeEntity fee1 = createFeeEntity(101L, ccd, 1L);
        FeeEntity fee2 = createFeeEntity(102L, ccd, 2L);
        FeeEntity fee3 = createFeeEntity(103L, ccd, 2L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link1, link2));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee1, fee2, fee3));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        // Keep link1 with fee1 (already there) and fee2 (move from link2)
        // Don't include fee3 - it should be deleted
        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of()), createFee(102L, List.of())),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(0, result.summary().feesToMove() == 1 ? 0 : -1); // fee2 moved
        assertEquals(1, result.summary().feesToMove());
        assertEquals(1, result.summary().feesToDelete()); // fee3 deleted
        assertEquals(1, result.summary().serviceRequestsToDelete()); // link2 deleted

        // Verify UPDATE for move
        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("UPDATE fee SET payment_link_id = 1 WHERE id = 102")));

        // Verify DELETE for fee3
        assertTrue(result.paymentDatabaseSql().stream()
            .anyMatch(sql -> sql.contains("DELETE FROM fee WHERE id = 103")));
    }

    @Test
    void testGenerateDeletionSql_noMoveWhenAlreadyOnCorrectLink() {
        String ccd = "1234567890123456";

        PaymentFeeLinkEntity link = createLink(1L, ccd, "PAY-001");
        FeeEntity fee = createFeeEntity(101L, ccd, 1L);

        when(paymentFeeLinkRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(link));
        when(feeRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of(fee));
        when(paymentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(remissionRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());
        when(apportionmentRepository.findByCcdCaseNumber(ccd)).thenReturn(List.of());

        CasePatchRequest request = new CasePatchRequest(
            ccd,
            List.of(createServiceRequest(
                1L, null,
                List.of(createFee(101L, List.of())),
                List.of()
            ))
        );

        SqlGenerationResult result = caseDiffService.generateDeletionSql(request);

        assertEquals(0, result.summary().feesToMove());
        assertEquals(0, result.summary().feesToDelete());
        assertTrue(result.paymentDatabaseSql().isEmpty());
    }
}