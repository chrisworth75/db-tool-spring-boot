package uk.gov.hmcts.reform.dbtool.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fee - clean domain representation
 */
@Data
@NoArgsConstructor
public class Fee {

    private String code;
    private String version;
    private Double amount;
    private Integer volume = 1;
    private String reference;
    private List<Remission> remissions = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Fee(String code, String version, Double amount) {
        this.code = code;
        this.version = version;
        this.amount = amount;
    }

    public void addRemission(Remission remission) {
        this.remissions.add(remission);
    }

    public double getTotalAmount() {
        return (amount != null ? amount : 0.0) * (volume != null ? volume : 1);
    }

    public double getAmountAfterRemissions() {
        double remissionTotal = remissions.stream()
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();
        return getTotalAmount() - remissionTotal;
    }
}
