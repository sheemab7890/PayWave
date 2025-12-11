package com.sheemab.shardedsagawallet.services.saga.steps;

import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.services.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SagaStepFactory {

    private final Map<String, SagaStepInterface> sagaStepMap;

    public static final List<SagaStepType> TransferMoneySagaSteps = List.of(
            SagaStepType.DEBIT_SOURCE_WALLET_STEP,
            SagaStepType.CREDIT_DESTINATION_WALLET_STEP,
            SagaStepType.UPDATE_TRANSACTION_STATUS_STEP
    );

    public SagaStepInterface getSagaStep(SagaStepType stepType) {
        SagaStepInterface step = sagaStepMap.get(stepType.name()); // use name() to match map key
        if (step == null) {
            throw new RuntimeException("Saga step not found: " + stepType.name());
        }
        return step;
    }
}
