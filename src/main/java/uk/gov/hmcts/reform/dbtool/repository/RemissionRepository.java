package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.RemissionEntity;

import java.util.List;

@Repository
public interface RemissionRepository extends JpaRepository<RemissionEntity, Long> {
    List<RemissionEntity> findByFeeId(Long feeId);
    List<RemissionEntity> findByFeeIdIn(List<Long> feeIds);
    List<RemissionEntity> findByCcdCaseNumber(String ccdCaseNumber);
}
