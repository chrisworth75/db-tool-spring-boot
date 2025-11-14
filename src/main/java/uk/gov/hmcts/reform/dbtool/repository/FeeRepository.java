package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.Fee;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByCcdCaseNumber(String ccdCaseNumber);
    List<Fee> findByPaymentLinkId(Long paymentLinkId);
    List<Fee> findByPaymentLinkIdIn(List<Long> paymentLinkIds);
}
