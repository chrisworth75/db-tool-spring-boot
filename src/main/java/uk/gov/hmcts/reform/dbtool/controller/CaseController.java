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
        int totalFees = 0;
        int totalPayments = 0;
        int totalRefunds = 0;
        int totalRemissions = 0;
        int serviceRequestCount = 0;
        int feeCount = 0;
        int paymentCount = 0;
        int refundCount = 0;
        int remissionCount = 0;

        for (Case c : cases) {
            CaseSummary summary = c.getSummary();
            totalFees += summary.totalFees();
            totalPayments += summary.totalPayments();
            totalRefunds += summary.totalRefunds();
            totalRemissions += summary.totalRemissions();
            serviceRequestCount += summary.serviceRequestCount();
            feeCount += summary.feeCount();
            paymentCount += summary.paymentCount();
            refundCount += summary.refundCount();
            remissionCount += summary.remissionCount();
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
                .remissionCount(remissionCount)
                .netAmount(totalPayments + totalRemissions - totalRefunds)
                .amountDue(totalFees - totalPayments - totalRemissions)
                .build();
    }
}