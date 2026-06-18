package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.entities.IdempotencyRecord;
import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.services.saga.SagaContext;
import com.sheemab.shardedsagawallet.services.saga.SagaOrchestrator;
import com.sheemab.shardedsagawallet.services.saga.steps.SagaStepFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaService {

    private final SagaOrchestrator sagaOrchestrator;
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public Long initiateTransfer(
            String idempotencyKey,
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description
    ) {
        log.info("[Transfer] Initiating ₹{} from wallet {} to wallet {}",
                amount, fromWalletId, toWalletId);

        // ── 1. Check if this key was already processed ────────────────────────
        Optional<IdempotencyRecord> existing =
                idempotencyService.checkIdempotency(idempotencyKey);

        if (existing.isPresent()) {
            // Duplicate request — return same result, no saga re-run
            log.info("[Transfer] Duplicate request. Returning sagaInstanceId: {}",
                    existing.get().getSagaInstanceId());
            return existing.get().getSagaInstanceId();
        }

        // ── 2. Lock this key — commits immediately (REQUIRES_NEW) ─────────────
        idempotencyService.markProcessing(idempotencyKey);

        Long sagaInstanceId = null;

        try {
            // ── 3. Create the transaction record ──────────────────────────────
            Transaction transaction = transactionService.createTransaction(
                    fromWalletId, toWalletId, amount, description
            );

            // ── 4. Build saga context ──────────────────────────────────────────
            SagaContext context = SagaContext.builder()
                    .data(Map.ofEntries(
                            Map.entry("transactionId",  transaction.getId()),
                            Map.entry("fromWalletId",   fromWalletId),
                            Map.entry("toWalletId",     toWalletId),
                            Map.entry("amount",         amount),
                            Map.entry("description",    description)
                    ))
                    .build();

            // ── 5. Start saga (saves SagaInstance to DB) ───────────────────────
            sagaInstanceId = sagaOrchestrator.startSaga(context);
            log.info("[Transfer] Saga started with ID: {}", sagaInstanceId);

            transactionService.updateTransactionWithSagaInstanceId(
                    transaction.getId(), sagaInstanceId
            );

            // ── 6. Execute all saga steps ──────────────────────────────────────
            boolean success = executeTransferSaga(sagaInstanceId);

            // ── 7. Update idempotency key based on outcome ─────────────────────
            if (success) {
                idempotencyService.markCompleted(idempotencyKey, sagaInstanceId);
                log.info("[Transfer] Completed successfully. sagaInstanceId: {}", sagaInstanceId);
            } else {
                idempotencyService.markFailed(idempotencyKey);
                log.warn("[Transfer] Saga failed. sagaInstanceId: {}", sagaInstanceId);
            }

            return sagaInstanceId;

        } catch (Exception e) {
            // Unexpected exception — mark failed so client can retry
            log.error("[Transfer] Unexpected error for sagaInstanceId: {}. Error: {}",
                    sagaInstanceId, e.getMessage(), e);
            idempotencyService.markFailed(idempotencyKey);
            throw e;
        }
    }

    /**
     * Executes all 3 saga steps in order.
     * Returns true if all steps passed, false if any step failed.
     */
    private boolean executeTransferSaga(Long sagaInstanceId) {
        log.info("[Saga] Executing sagaInstanceId: {}", sagaInstanceId);

        try {
            for (SagaStepType stepType : SagaStepFactory.TransferMoneySagaSteps) {
                boolean stepPassed = sagaOrchestrator.executeStep(
                        sagaInstanceId, stepType.toString()
                );

                if (!stepPassed) {
                    log.error("[Saga] Step {} failed. Triggering compensation.", stepType);
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return false;
                }
            }

            sagaOrchestrator.completeSaga(sagaInstanceId);
            return true;

        } catch (Exception e) {
            log.error("[Saga] Unexpected error at sagaInstanceId: {}", sagaInstanceId, e);
            sagaOrchestrator.failSaga(sagaInstanceId);
            return false;
        }
    }
}
