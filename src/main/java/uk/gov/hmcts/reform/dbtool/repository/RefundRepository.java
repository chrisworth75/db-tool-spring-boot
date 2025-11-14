package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.Refund;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByCcdCaseNumber(String ccdCaseNumber);
    List<Refund> findByPaymentReference(String paymentReference);
    List<Refund> findByPaymentReferenceIn(List<String> paymentReferences);
}
