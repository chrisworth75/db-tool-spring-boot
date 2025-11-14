package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.PaymentFeeLink;

import java.util.List;

@Repository
public interface PaymentFeeLinkRepository extends JpaRepository<PaymentFeeLink, Long> {
    List<PaymentFeeLink> findByCcdCaseNumber(String ccdCaseNumber);
    List<PaymentFeeLink> findByPaymentReference(String paymentReference);
}
