package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.Apportionment;

import java.util.List;

@Repository
public interface ApportionmentRepository extends JpaRepository<Apportionment, Long> {
    List<Apportionment> findByPaymentId(Long paymentId);
    List<Apportionment> findByPaymentIdIn(List<Long> paymentIds);
    List<Apportionment> findByCcdCaseNumber(String ccdCaseNumber);
}
