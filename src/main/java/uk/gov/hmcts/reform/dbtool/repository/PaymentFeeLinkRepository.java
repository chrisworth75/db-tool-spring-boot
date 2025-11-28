package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.PaymentFeeLinkEntity;

import java.util.List;

@Repository
public interface PaymentFeeLinkRepository extends JpaRepository<PaymentFeeLinkEntity, Long> {
    List<PaymentFeeLinkEntity> findByCcdCaseNumber(String ccdCaseNumber);
    List<PaymentFeeLinkEntity> findByPaymentReference(String paymentReference);
}
