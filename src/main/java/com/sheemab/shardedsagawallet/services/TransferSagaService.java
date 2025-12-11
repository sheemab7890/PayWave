package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.services.saga.SagaContext;
import com.sheemab.shardedsagawallet.services.saga.SagaOrchestrator;
import com.sheemab.shardedsagawallet.services.saga.steps.SagaStepFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaService {

    private final SagaOrchestrator sagaOrchestrator;
    private final TransactionService transactionService;

    @Transactional
    public Long initiateTransfer(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    ) {
        log.info("Initiating transfer of {} from wallet {} to wallet {} ", amount, fromWalletId, toWalletId);

        Transaction transaction = transactionService.createTransaction(
                fromWalletId,
                toWalletId,
                amount,
                description
        );

        SagaContext context = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("fromWalletId", fromWalletId),
                        Map.entry("toWalletId", toWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)
                ))
                .build();

        Long sagaInstanceId = sagaOrchestrator.startSaga(context);
        log.info("Saga instance created with ID: {} ", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        executeTransferSaga(sagaInstanceId);

        return sagaInstanceId;
    }

    public void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing transfer saga with ID: {} ", sagaInstanceId);

        try {
            for(SagaStepType stepType: SagaStepFactory.TransferMoneySagaSteps) {
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, stepType.toString());
                if (!success) {
                    log.error("Failed to execute step {}", stepType.toString());
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }
            }
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga with ID: {} completed successfully", sagaInstanceId);
        } catch (Exception e) {
            log.error("Failed to execute transfer saga with ID: {}. Error: {} ", sagaInstanceId, e.getMessage(), e);
            sagaOrchestrator.failSaga(sagaInstanceId);

        }
    }
}
