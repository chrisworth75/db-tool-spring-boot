package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCcdCaseNumber(String ccdCaseNumber);
    List<Payment> findByPaymentLinkId(Long paymentLinkId);
    List<Payment> findByPaymentLinkIdIn(List<Long> paymentLinkIds);
    List<Payment> findByReference(String reference);
}
