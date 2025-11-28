package uk.gov.hmcts.reform.dbtool.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    void testCaseSummaryBuilder() {
        CaseSummary summary = CaseSummary.builder()
            .totalFees(100)
            .totalPayments(90)
            .totalRefunds(10)
            .totalRemissions(5)
            .serviceRequestCount(1)
            .feeCount(2)
            .paymentCount(3)
            .refundCount(1)
            .remissionCount(1)
            .netAmount(85)
            .amountDue(5)
            .build();

        assertEquals(100, summary.totalFees());
        assertEquals(90, summary.totalPayments());
        assertEquals(10, summary.totalRefunds());
        assertEquals(5, summary.totalRemissions());
        assertEquals(1, summary.serviceRequestCount());
        assertEquals(2, summary.feeCount());
        assertEquals(3, summary.paymentCount());
        assertEquals(1, summary.refundCount());
        assertEquals(1, summary.remissionCount());
        assertEquals(85, summary.netAmount());
        assertEquals(5, summary.amountDue());
    }

    @Test
    void testServiceRequestRecord() {
        ServiceRequest sr = new ServiceRequest(
                null, null, null, null,
                List.of(), List.of(),
                null, null, null, null, null
        );
        assertNotNull(sr.fees());
        assertNotNull(sr.payments());

        ServiceRequest sr2 = new ServiceRequest(
                null, "PAY-123", "1234567890123456", null,
                null, null,
                null, null, null, null, null
        );
        assertEquals("PAY-123", sr2.paymentReference());
        assertEquals("1234567890123456", sr2.ccdCaseNumber());
        assertNotNull(sr2.fees());
        assertNotNull(sr2.payments());
    }

    @Test
    void testServiceRequestWithFeesAndPayments() {
        Fee fee = new Fee(null, "FEE001", null, null, null, null, null, null, null, List.of(), null, null);
        Payment payment = new Payment(null, "RC-123", null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of());

        ServiceRequest sr = new ServiceRequest(
                null, null, null, null,
                List.of(fee), List.of(payment),
                null, null, null, null, null
        );

        assertEquals(1, sr.fees().size());
        assertEquals(1, sr.payments().size());
    }

    @Test
    void testFeeWithRemission() {
        Remission remission = new Remission("HWF-123", null, null, null, null);
        Fee fee = new Fee(null, null, null, null, null, null, null, null, null, List.of(remission), null, null);

        assertEquals(1, fee.remissions().size());
        assertEquals("HWF-123", fee.remissions().get(0).hwfReference());
    }

    @Test
    void testPaymentWithRefundsAndApportionments() {
        Refund refund = new Refund("REF-123", null, null, null, null, null, null, null, null);
        Apportionment apportionment = new Apportionment(null, null, "100.00", null, null, null, null);

        Payment payment = new Payment(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(refund), List.of(apportionment));

        assertEquals(1, payment.refunds().size());
        assertEquals(1, payment.apportionments().size());
    }

    @Test
    void testApportionmentRecord() {
        LocalDateTime now = LocalDateTime.now();

        Apportionment apportionment = new Apportionment(
                1L, 2L, "100.00", "AUTO", "0.00", now, now
        );

        assertEquals(1L, apportionment.id());
        assertEquals(2L, apportionment.feeId());
        assertEquals("100.00", apportionment.apportionAmount());
        assertEquals("AUTO", apportionment.apportionType());
        assertEquals("0.00", apportionment.callSurplusAmount());
        assertEquals(now, apportionment.dateCreated());
        assertEquals(now, apportionment.dateUpdated());
    }

    @Test
    void testRefundRecord() {
        LocalDateTime now = LocalDateTime.now();

        Refund refund = new Refund(
                "REF-123", 50.0, "Overpayment", "Approved", "AUTOMATED",
                now, now, "system", "admin"
        );

        assertEquals("REF-123", refund.reference());
        assertEquals(50.0, refund.amount());
        assertEquals("Overpayment", refund.reason());
        assertEquals("Approved", refund.status());
        assertEquals("AUTOMATED", refund.instructionType());
        assertEquals(now, refund.createdAt());
        assertEquals(now, refund.updatedAt());
        assertEquals("system", refund.createdBy());
        assertEquals("admin", refund.updatedBy());
    }

    @Test
    void testRemissionRecord() {
        LocalDateTime now = LocalDateTime.now();

        Remission remission = new Remission(
                "HWF-123", 25.0, "John Doe", now, now
        );

        assertEquals("HWF-123", remission.hwfReference());
        assertEquals(25.0, remission.amount());
        assertEquals("John Doe", remission.beneficiaryName());
        assertEquals(now, remission.createdAt());
        assertEquals(now, remission.updatedAt());
    }

    @Test
    void testFeeRecord() {
        LocalDateTime now = LocalDateTime.now();

        Fee fee = new Fee(
                1L, "FEE001", "1", "100.00", "100.00", "100.00", "100.00",
                1, "Fee ref", List.of(), now, now
        );

        assertEquals(1L, fee.id());
        assertEquals("FEE001", fee.code());
        assertEquals("1", fee.version());
        assertEquals("100.00", fee.amount());
        assertEquals("100.00", fee.calculatedAmount());
        assertEquals("100.00", fee.netAmount());
        assertEquals("100.00", fee.amountDue());
        assertEquals(1, fee.volume());
        assertEquals("Fee ref", fee.reference());
        assertEquals(now, fee.dateCreated());
        assertEquals(now, fee.dateUpdated());
    }

    @Test
    void testPaymentRecord() {
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(
                1L, "RC-123", "100.00", "GBP", "Success", "Card", "Provider", "Online",
                "EXT-123", "CUST-123", "PBA123", "John Doe",
                now, now, now, List.of(), List.of()
        );

        assertEquals(1L, payment.id());
        assertEquals("RC-123", payment.reference());
        assertEquals("100.00", payment.amount());
        assertEquals("GBP", payment.currency());
        assertEquals("Success", payment.status());
        assertEquals("Card", payment.method());
        assertEquals("Provider", payment.provider());
        assertEquals("Online", payment.channel());
        assertEquals("EXT-123", payment.externalReference());
        assertEquals("CUST-123", payment.customerReference());
        assertEquals("PBA123", payment.pbaNumber());
        assertEquals("John Doe", payment.payerName());
        assertEquals(now, payment.dateCreated());
        assertEquals(now, payment.dateUpdated());
        assertEquals(now, payment.bankedDate());
    }

    @Test
    void testServiceRequestRecord_fullFields() {
        LocalDateTime now = LocalDateTime.now();

        ServiceRequest sr = new ServiceRequest(
                1L, "PAY-123", "1234567890123456", "CASE-123",
                List.of(), List.of(),
                now, now, "ORG-1", "Service", "http://callback"
        );

        assertEquals(1L, sr.id());
        assertEquals("PAY-123", sr.paymentReference());
        assertEquals("1234567890123456", sr.ccdCaseNumber());
        assertEquals("CASE-123", sr.caseReference());
        assertEquals(now, sr.dateCreated());
        assertEquals(now, sr.dateUpdated());
        assertEquals("ORG-1", sr.orgId());
        assertEquals("Service", sr.enterpriseServiceName());
        assertEquals("http://callback", sr.serviceRequestCallbackUrl());
    }
}