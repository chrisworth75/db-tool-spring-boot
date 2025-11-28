package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.ApportionmentEntity;

import java.util.List;

@Repository
public interface ApportionmentRepository extends JpaRepository<ApportionmentEntity, Long> {
    List<ApportionmentEntity> findByPaymentId(Long paymentId);
    List<ApportionmentEntity> findByPaymentIdIn(List<Long> paymentIds);
    List<ApportionmentEntity> findByCcdCaseNumber(String ccdCaseNumber);
}
