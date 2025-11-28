package uk.gov.hmcts.reform.dbtool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.dbtool.domain.Case;
import uk.gov.hmcts.reform.dbtool.domain.CaseSummary;
import uk.gov.hmcts.reform.dbtool.service.CaseQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class DbToolApplication {

    private final CaseQueryService caseQueryService;
    private final ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(DbToolApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            String ccd = null;

            // Parse command line arguments
            for (int i = 0; i < args.length; i++) {
                if ("--ccd".equals(args[i]) && i + 1 < args.length) {
                    ccd = args[i + 1];
                }
            }

            if (ccd == null) {
                System.err.println("Usage: java -jar db-tool-spring-boot.jar --ccd <CCD_NUMBER>");
                System.exit(1);
            }

            try {
                System.err.println("Fetching all data for CCD: " + ccd);
                List<Case> cases = caseQueryService.queryCaseByCcd(ccd);

                if (cases.isEmpty()) {
                    System.err.println("No data found");
                    System.exit(1);
                }

                // Build output
                Map<String, Object> output = new HashMap<>();
                if (cases.size() == 1) {
                    Case singleCase = cases.get(0);
                    output.put("case", singleCase);
                    output.put("summary", singleCase.getSummary());
                } else {
                    output.put("cases", cases);
                    CaseSummary combinedSummary = calculateCombinedSummary(cases);
                    output.put("summary", combinedSummary);
                }

                // Output JSON to stdout
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
                System.out.println(json);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
        };
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
