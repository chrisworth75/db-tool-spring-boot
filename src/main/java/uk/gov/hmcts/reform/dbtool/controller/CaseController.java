package uk.gov.hmcts.reform.dbtool.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.domain.CasePatchRequest;
import uk.gov.hmcts.reform.dbtool.domain.CaseSummary;
import uk.gov.hmcts.reform.dbtool.domain.SqlGenerationResult;
import uk.gov.hmcts.reform.dbtool.service.CaseDiffService;
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
    private final CaseDiffService caseDiffService;

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

    /**
     * PATCH /api/cases/ccd/{ccdCaseNumber}
     * Compare the provided case structure with the database and return SQL to delete
     * entities that are missing from the patch request.
     *
     * The request body should contain only the entities to KEEP.
     * Any entities present in the database but absent from the request will be marked for deletion.
     *
     * Returns SQL statements (not executed) that can be run later to make the changes.
     */
    @PatchMapping("/ccd/{ccdCaseNumber}")
    public ResponseEntity<SqlGenerationResult> patchCase(
            @PathVariable String ccdCaseNumber,
            @RequestBody CasePatchRequest patchRequest) {

        log.info("PATCH /api/cases/ccd/{}", ccdCaseNumber);

        // Validate that the path variable matches the request body
        if (patchRequest.ccdCaseNumber() == null || !patchRequest.ccdCaseNumber().equals(ccdCaseNumber)) {
            log.warn("CCD case number mismatch: path={}, body={}", ccdCaseNumber, patchRequest.ccdCaseNumber());
            return ResponseEntity.badRequest().build();
        }

        // Check if case exists
        List<Case> existingCases = caseQueryService.queryCaseByCcd(ccdCaseNumber);
        if (existingCases.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SqlGenerationResult result = caseDiffService.generateDeletionSql(patchRequest);
        return ResponseEntity.ok(result);
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