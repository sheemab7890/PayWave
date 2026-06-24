package com.sheemab.shardedsagawallet.services.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheemab.shardedsagawallet.entities.SagaInstance;
import com.sheemab.shardedsagawallet.entities.SagaStep;
import com.sheemab.shardedsagawallet.enums.SagaStatus;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.enums.StepStatus;
import com.sheemab.shardedsagawallet.repositories.SagaInstanceRepository;
import com.sheemab.shardedsagawallet.repositories.SagaStepRepository;
import com.sheemab.shardedsagawallet.services.saga.steps.SagaStepFactory;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator{

    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {

        try {
            String contextJson = objectMapper.writeValueAsString(context); // Java Object -> JSON String
            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();

           sagaInstance = sagaInstanceRepository.save(sagaInstance);

            log.info("Started new saga with ID: {}", sagaInstance.getId());

            return sagaInstance.getId();

        } catch (Exception e) {
            log.error("Failed to start saga: {}", e.getMessage());
            throw new RuntimeException("Failed to start saga", e);
        }
    }


    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) {

        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));

        SagaStepInterface step = sagaStepFactory.getSagaStep(SagaStepType.valueOf(stepName)); // Gives the corresponding step name
        if (step == null) {
            log.error("Saga step not found: {}", stepName);
            throw new RuntimeException("Saga step not found: " + stepName);
        }
        
        SagaStep sagaStepDB = sagaStepRepository.findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING)
                .orElse(SagaStep.builder()
                        .sagaInstanceId(sagaInstanceId)
                        .stepName(stepName)
                        .status(StepStatus.PENDING)
                        .build());

        if (sagaStepDB.getId() == null) {
            sagaStepRepository.save(sagaStepDB);
        }

        try {
            SagaContext context = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);   // Get the context from JSON String -> Java Object
            sagaStepDB.markAsRunning();
            sagaStepRepository.save(sagaStepDB); // Update step status to RUNNING in DB

           boolean success = step.execute(context); // Execute the step with the context
            if(success){
                sagaStepDB.markAsCompleted();
                sagaStepRepository.save(sagaStepDB);

                sagaInstance.setCurrentStep(stepName);
                sagaInstance.setStatus(SagaStatus.RUNNING);
                sagaInstanceRepository.save(sagaInstance); // Update saga instance with current step and status

                log.info("Step {} executed successfully for SagaInstance ID {}", stepName, sagaInstanceId);
                return true;
            }else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB);
                log.error("Step {} execution failed for SagaInstance ID {}", stepName, sagaInstanceId);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {} for SagaInstance ID {}: {}", stepName, sagaInstanceId, e.getMessage());
            return false;
        }

    }

    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) {
        // 1. Fetch the saga instance from db using sagaInstanceId
          SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));

        SagaStepInterface step = sagaStepFactory.getSagaStep(SagaStepType.valueOf(stepName)); // Gives the corresponding step name
        if (step == null) {
            log.error("Saga step not found: {}", stepName);
            throw new RuntimeException("Saga step not found: " + stepName);
        }

        // 2. Fetch the saga step from db using sagaInstanceId and stepName
        SagaStep sagaStepDB = sagaStepRepository.findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED)
                .orElse(null); // no such step found in db

        if(sagaStepDB == null){
            log.warn("Step {} not found or not completed for SagaInstance ID {}", stepName, sagaInstanceId);
            return true; // nothing to compensate
        }

        // 3. Take the context from sagaInstance and call the compensate method
        try {
            SagaContext context = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);   // Get the context from JSON String -> Java Object
            sagaStepDB.markAsCompensating();
            sagaStepRepository.save(sagaStepDB); // Update step status to RUNNING in DB

            boolean success = step.compensate(context); // Execute the step with the context
            if(success){
                sagaStepDB.markAsCompensated();
                sagaStepRepository.save(sagaStepDB);
                log.info("Step {} compensating successfully for SagaInstance ID {}", stepName, sagaInstanceId);
                return true;
            }else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB);
                log.error("Step {} execution failed for SagaInstance ID {}", stepName, sagaInstanceId);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {} for SagaInstance ID {}: {}", stepName, sagaInstanceId, e.getMessage());
            return false;
        }

    }

    @Override
    @Transactional
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));

    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        // Get the sagaInstance
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));

        // Mark the sagaInstance status as COMPENSATING
        sagaInstance.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(sagaInstance);

        // Fetch all the steps with status COMPLETED
        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId, StepStatus.COMPLETED);

        // Compensate each step
        boolean allCompensated = true;
        for(SagaStep completedStep : completedSteps){
            boolean compensated = this.compensateStep(sagaInstanceId, completedStep.getStepName());
            if(!compensated){
                allCompensated = false;
            }
        }

        if(allCompensated){
            sagaInstance.setStatus(SagaStatus.COMPENSATED);
            sagaInstanceRepository.save(sagaInstance);
            log.info("Saga {} compensated successfully", sagaInstanceId);
        } else {
           log.error("Saga {} compensation failed", sagaInstanceId);
        }

    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
       SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));
        sagaInstance.setStatus(SagaStatus.FAILED);
        sagaInstanceRepository.save(sagaInstance);

        compensateSaga(sagaInstanceId);
        log.info("Saga {} failed", sagaInstanceId);

    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("SagaInstance not found with ID: " + sagaInstanceId));
        sagaInstance.setStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(sagaInstance);
    }
}
