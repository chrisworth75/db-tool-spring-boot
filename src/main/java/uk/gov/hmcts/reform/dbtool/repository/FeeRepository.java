package uk.gov.hmcts.reform.dbtool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dbtool.database.FeeEntity;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<FeeEntity, Long> {
    List<FeeEntity> findByCcdCaseNumber(String ccdCaseNumber);
}
