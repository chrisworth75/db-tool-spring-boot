package uk.gov.hmcts.reform.dbtool.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.domain.CaseSummary;
import uk.gov.hmcts.reform.dbtool.service.CaseQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for querying case data
 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Slf4j
public class CaseController {

    private final CaseQueryService caseQueryService;

    /**
     * GET /api/cases/ccd/{ccdCaseNumber}
     * Query case by CCD case number
     */
    @GetMapping("/ccd/{ccdCaseNumber}")
    public ResponseEntity<Map<String, Object>> getCaseByCcd(
            @PathVariable String ccdCaseNumber) {

        log.info("GET /api/cases/ccd/{}", ccdCaseNumber);

        List<Case> cases = caseQueryService.queryCaseByCcd(ccdCaseNumber);

        if (cases.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Return single case if only one, otherwise return array
        Map<String, Object> response = new HashMap<>();
        if (cases.size() == 1) {
            Case singleCase = cases.get(0);
            response.put("case", singleCase);
            response.put("summary", singleCase.getSummary());
        } else {
            response.put("cases", cases);
            // Calculate combined summary
            CaseSummary combinedSummary = calculateCombinedSummary(cases);
            response.put("summary", combinedSummary);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cases/payment-reference/{paymentReference}
     * Query case by payment reference
     */
    @GetMapping("/payment-reference/{paymentReference}")
    public ResponseEntity<Map<String, Object>> getCaseByPaymentReference(
            @PathVariable String paymentReference) {

        log.info("GET /api/cases/payment-reference/{}", paymentReference);

        List<Case> cases = caseQueryService.queryCaseByPaymentReference(paymentReference);

        if (cases.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        if (cases.size() == 1) {
            Case singleCase = cases.get(0);
            response.put("case", singleCase);
            response.put("summary", singleCase.getSummary());
        } else {
            response.put("cases", cases);
            CaseSummary combinedSummary = calculateCombinedSummary(cases);
            response.put("summary", combinedSummary);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cases/ccd/{ccdCaseNumber}/summary
     * Get summary only for a case
     */
    @GetMapping("/ccd/{ccdCaseNumber}/summary")
    public ResponseEntity<CaseSummary> getCaseSummary(
            @PathVariable String ccdCaseNumber) {

        log.info("GET /api/cases/ccd/{}/summary", ccdCaseNumber);

        List<Case> cases = caseQueryService.queryCaseByCcd(ccdCaseNumber);

        if (cases.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CaseSummary summary = cases.size() == 1 ?
                cases.get(0).getSummary() : calculateCombinedSummary(cases);

        return ResponseEntity.ok(summary);
    }

    private CaseSummary calculateCombinedSummary(List<Case> cases) {
        double totalFees = 0;
        double totalPayments = 0;
        double totalRefunds = 0;
        double totalRemissions = 0;
        int serviceRequestCount = 0;
        int feeCount = 0;
        int paymentCount = 0;
        int refundCount = 0;

        for (Case c : cases) {
            CaseSummary summary = c.getSummary();
            totalFees += summary.getTotalFees();
            totalPayments += summary.getTotalPayments();
            totalRefunds += summary.getTotalRefunds();
            totalRemissions += summary.getTotalRemissions();
            serviceRequestCount += summary.getServiceRequestCount();
            feeCount += summary.getFeeCount();
            paymentCount += summary.getPaymentCount();
            refundCount += summary.getRefundCount();
        }

        return CaseSummary.builder()
                .totalFees(totalFees)
                .totalPayments(totalPayments)
                .totalRefunds(totalRefunds)
                .totalRemissions(totalRemissions)
                .serviceRequestCount(serviceRequestCount)
                .feeCount(feeCount)
                .paymentCount(paymentCount)
                .refundCount(refundCount)
                .netAmount(totalPayments + totalRemissions - totalRefunds)
                .amountDue(totalFees - totalPayments - totalRemissions)
                .build();
    }
}
