package com.sheemab.shardedsagawallet.services.saga.steps;

import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.enums.TransactionStatus;
import com.sheemab.shardedsagawallet.repositories.TransactionRepository;
import com.sheemab.shardedsagawallet.services.saga.SagaContext;
import com.sheemab.shardedsagawallet.services.saga.SagaStepInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Saga Step: Update transaction status after a successful money transfer.
 *
 * ➤ Purpose:
 *   This step updates the Transaction record to mark it as SUCCESS once
 *   all other Saga steps (e.g., debit, credit) complete successfully.
 *
 * ➤ Compensation:
 *   If any later Saga step fails, this reverts the status back to its
 *   original value stored in SagaContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatus implements SagaStepInterface {

    private final TransactionRepository transactionRepository;

    /**
     * Executes the Saga step:
     *  1️⃣ Fetch transaction by ID
     *  2️⃣ Store original status in context (for rollback)
     *  3️⃣ Mark transaction as SUCCESS
     *  4️⃣ Save and log results
     */
    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        try {
            Long transactionId = context.getLong("transactionId");

            // Safety check — ensure ID exists in context
            if (transactionId == null) {
                throw new IllegalArgumentException("Transaction ID missing in SagaContext");
            }

            log.info("[Saga] 🚀 Updating transaction status for ID {}", transactionId);

            // Step 1: Fetch transaction
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

            // Step 2: Store original status for possible compensation
            context.put("originalTransactionStatus", transaction.getStatus());

            // Step 3: Only update if not already SUCCESS (idempotency)
            if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(transaction);
                log.info("[Saga] ✅ Transaction marked SUCCESS for ID {}", transactionId);
            } else {
                log.info("[Saga] ℹ️ Transaction already SUCCESS for ID {}, skipping update", transactionId);
            }

            // Step 4: Store updated status in context
            context.put("updatedTransactionStatus", transaction.getStatus());

            return true;

        } catch (Exception e) {
            log.error("[Saga] ❌ Failed to update transaction status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compensation step:
     *  If Saga fails later, revert transaction status to its original state.
     *
     *  1️⃣ Fetch transaction again
     *  2️⃣ Restore original status from context
     *  3️⃣ Save and log
     */
    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            Long transactionId = context.getLong("transactionId");
            TransactionStatus originalTransactionStatus =
                    (TransactionStatus) context.get("originalTransactionStatus");

            if (transactionId == null) {
                throw new IllegalArgumentException("Transaction ID missing in SagaContext");
            }

            log.warn("[Saga-Compensation] 🔁 Reverting transaction status for ID {} to {}",
                    transactionId, originalTransactionStatus);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

            // Fallback to FAILED if original status missing
            TransactionStatus revertStatus = (originalTransactionStatus != null)
                    ? originalTransactionStatus
                    : TransactionStatus.FAILED;

            transaction.setStatus(revertStatus);
            transactionRepository.save(transaction);

            log.info("[Saga-Compensation] ✅ Transaction status reverted successfully for ID {}", transactionId);
            return true;

        } catch (Exception e) {
            log.error("[Saga-Compensation] ❌ Failed to revert transaction status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Each Saga step must have a unique name for tracking and recovery.
     */
    @Override
    public String getStepName() {
        return SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString();
    }
}