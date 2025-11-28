package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.RefundEntity;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<RefundEntity, Long> {
    List<RefundEntity> findByCcdCaseNumber(String ccdCaseNumber);
    List<RefundEntity> findByPaymentReference(String paymentReference);
    List<RefundEntity> findByPaymentReferenceIn(List<String> paymentReferences);
}
