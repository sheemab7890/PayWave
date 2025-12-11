package com.sheemab.shardedsagawallet.repositories;

import com.sheemab.shardedsagawallet.entities.SagaStep;
import com.sheemab.shardedsagawallet.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    //List<SagaStep> findBySagaInstanceIdAndStatus(Long sagaInstanceId, StepStatus status);

    @Query("SELECT s FROM SagaStep s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status = StepStatus.COMPLETED")
    List<SagaStep> findCompletedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);

//    @Query("SELECT s FROM SagaStep s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status IN ('COMPLETED', 'COMPENSATED')")
//    List<SagaStep> findCompletedOrCompensatedStepsBySagaInstanceId(Long sagaInstanceId);

    Optional<SagaStep> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, String stepName, StepStatus status);
}
