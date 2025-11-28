package uk.gov.hmcts.reform.dbtool.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseTest {

    @Test
    void testConstructorWithCcd() {
        String ccd = "1234567890123456";
        Case testCase = new Case(ccd);

        assertEquals(ccd, testCase.getCcdCaseNumber());
        assertNotNull(testCase.getServiceRequests());
        assertTrue(testCase.getServiceRequests().isEmpty());
    }

    @Test
    void testNoArgsConstructor() {
        Case testCase = new Case();

        assertNotNull(testCase.getServiceRequests());
        assertTrue(testCase.getServiceRequests().isEmpty());
    }

    @Test
    void testAddServiceRequest() {
        Case testCase = new Case("1234567890123456");
        ServiceRequest sr = new ServiceRequest(
                null, "PAY-123", null, null,
                List.of(), List.of(),
                null, null, null, null, null
        );

        testCase.addServiceRequest(sr);

        assertEquals(1, testCase.getServiceRequests().size());
        assertEquals("PAY-123", testCase.getServiceRequests().get(0).paymentReference());
    }

    @Test
    void testGetSummary_emptyCase() {
        Case testCase = new Case("1234567890123456");

        CaseSummary summary = testCase.getSummary();

        assertNotNull(summary);
        assertEquals(0, summary.totalFees());
        assertEquals(0, summary.totalPayments());
        assertEquals(0, summary.totalRefunds());
        assertEquals(0, summary.totalRemissions());
        assertEquals(0, summary.serviceRequestCount());
        assertEquals(0, summary.feeCount());
        assertEquals(0, summary.paymentCount());
        assertEquals(0, summary.refundCount());
        assertEquals(0, summary.remissionCount());
        assertEquals(0, summary.netAmount());
        assertEquals(0, summary.amountDue());
    }

    @Test
    void testGetSummary_withData() {
        Case testCase = new Case("1234567890123456");

        Remission remission = new Remission(null, 25.0, null, null, null);
        Fee fee = new Fee(null, "FEE001", null, "100.00", null, null, null, null, null,
                List.of(remission), null, null);

        Refund refund = new Refund(null, 10.0, null, null, null, null, null, null, null);
        Payment payment = new Payment(null, "RC-123", "100.00", null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(refund), List.of());

        ServiceRequest sr = new ServiceRequest(
                null, "PAY-123", null, null,
                List.of(fee), List.of(payment),
                null, null, null, null, null
        );

        testCase.addServiceRequest(sr);

        CaseSummary summary = testCase.getSummary();

        assertNotNull(summary);
        assertEquals(100, summary.totalFees());
        assertEquals(100, summary.totalPayments());
        assertEquals(10, summary.totalRefunds());
        assertEquals(25, summary.totalRemissions());
        assertEquals(1, summary.serviceRequestCount());
        assertEquals(1, summary.feeCount());
        assertEquals(1, summary.paymentCount());
        assertEquals(1, summary.refundCount());
        assertEquals(1, summary.remissionCount());
        assertEquals(115, summary.netAmount()); // 100 payment + 25 remission - 10 refund
        assertEquals(-25, summary.amountDue()); // 100 fee - 100 payment - 25 remission
    }

    @Test
    void testGetSummary_withNullAmounts() {
        Case testCase = new Case("1234567890123456");

        Remission remission = new Remission(null, null, null, null, null);
        Fee fee = new Fee(null, null, null, null, null, null, null, null, null,
                List.of(remission), null, null);

        Refund refund = new Refund(null, null, null, null, null, null, null, null, null);
        Payment payment = new Payment(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(refund), List.of());

        ServiceRequest sr = new ServiceRequest(
                null, null, null, null,
                List.of(fee), List.of(payment),
                null, null, null, null, null
        );

        testCase.addServiceRequest(sr);

        CaseSummary summary = testCase.getSummary();

        assertNotNull(summary);
        assertEquals(0, summary.totalFees());
        assertEquals(0, summary.totalPayments());
        assertEquals(0, summary.totalRefunds());
        assertEquals(0, summary.totalRemissions());
    }

    @Test
    void testGetSummary_withInvalidAmounts() {
        Case testCase = new Case("1234567890123456");

        Fee fee = new Fee(null, null, null, "invalid", null, null, null, null, null,
                List.of(), null, null);

        Payment payment = new Payment(null, null, "not-a-number", null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of());

        ServiceRequest sr = new ServiceRequest(
                null, null, null, null,
                List.of(fee), List.of(payment),
                null, null, null, null, null
        );

        testCase.addServiceRequest(sr);

        CaseSummary summary = testCase.getSummary();

        assertNotNull(summary);
        assertEquals(0, summary.totalFees());
        assertEquals(0, summary.totalPayments());
    }

    @Test
    void testGetSummary_multipleServiceRequests() {
        Case testCase = new Case("1234567890123456");

        Fee fee1 = new Fee(null, null, null, "50.00", null, null, null, null, null,
                List.of(), null, null);
        Payment payment1 = new Payment(null, null, "50.00", null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of());
        ServiceRequest sr1 = new ServiceRequest(
                null, null, null, null,
                List.of(fee1), List.of(payment1),
                null, null, null, null, null
        );

        Fee fee2 = new Fee(null, null, null, "75.50", null, null, null, null, null,
                List.of(), null, null);
        Payment payment2 = new Payment(null, null, "75.50", null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of());
        ServiceRequest sr2 = new ServiceRequest(
                null, null, null, null,
                List.of(fee2), List.of(payment2),
                null, null, null, null, null
        );

        testCase.addServiceRequest(sr1);
        testCase.addServiceRequest(sr2);

        CaseSummary summary = testCase.getSummary();

        assertNotNull(summary);
        assertEquals(125, summary.totalFees()); // 50 + 75.50 = 125
        assertEquals(125, summary.totalPayments());
        assertEquals(2, summary.serviceRequestCount());
        assertEquals(2, summary.feeCount());
        assertEquals(2, summary.paymentCount());
    }

    @Test
    void testSettersAndGetters() {
        Case testCase = new Case();
        testCase.setCcdCaseNumber("9876543210987654");

        assertEquals("9876543210987654", testCase.getCcdCaseNumber());
    }
}