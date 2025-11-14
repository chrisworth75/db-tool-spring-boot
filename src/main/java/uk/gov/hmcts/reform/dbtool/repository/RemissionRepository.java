package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.Remission;

import java.util.List;

@Repository
public interface RemissionRepository extends JpaRepository<Remission, Long> {
    List<Remission> findByFeeId(Long feeId);
    List<Remission> findByFeeIdIn(List<Long> feeIds);
    List<Remission> findByCcdCaseNumber(String ccdCaseNumber);
}
