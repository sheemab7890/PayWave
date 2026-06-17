package com.sheemab.shardedsagawallet.services.saga.steps;

import com.sheemab.shardedsagawallet.entities.Wallet;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.repositories.WalletRepository;
import com.sheemab.shardedsagawallet.services.saga.SagaContext;
import com.sheemab.shardedsagawallet.services.saga.SagaStepInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**

 * ✅ Purpose:
 * Saga step responsible for DEBITING money from the source wallet
 * as part of a distributed money transfer transaction.

 * 🔁 Saga Flow:
 *  1️⃣ execute()     → Perform the debit operation
 *  2️⃣ compensate()  → Reverse the debit if any later step fails (undo)

 * 🧠 Key Idea:
 *  Uses PESSIMISTIC LOCKING to prevent concurrent balance updates.
 *  Uses SagaContext to store temporary data for audit & compensation.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitSourceWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;

    /**
     * Executes the debit step of the Saga.
     *
     * Steps:
     *  1️⃣ Fetch the source wallet with a pessimistic lock (to prevent parallel modifications)
     *  2️⃣ Store the original balance for rollback
     *  3️⃣ Perform the debit (may throw if insufficient balance)
     *  4️⃣ Save updated wallet
     *  5️⃣ Store new balance in SagaContext for audit
     */
    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        try {
            // 1️⃣ Extract data from SagaContext (shared memory)
            Long fromWalletId = context.getLong("fromWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (fromWalletId == null || amount == null) {
                throw new IllegalArgumentException("Missing 'fromWalletId' or 'amount' in SagaContext");
            }

            log.info("[Saga-Step: Debit] 🚀 Initiating debit of ₹{} from wallet ID {}", amount, fromWalletId);

            // 2️⃣ Fetch wallet from DB with pessimistic write lock
            Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                    .orElseThrow(() -> new IllegalStateException("Source wallet not found: ID = " + fromWalletId));

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient balance");
            }

            log.debug("[Saga-Step: Debit] Current balance before debit: {}", wallet.getBalance());

            // 3️⃣ Backup original balance for compensation
            context.put("originalSourceWalletBalance", wallet.getBalance());

            // 4️⃣ Perform debit in memory (throws if insufficient balance)
            BigDecimal newBalance = wallet.getBalance().subtract(amount);

            // 5️⃣ Save updated wallet in DB
            walletRepository.updateBalance(fromWalletId, newBalance);

            log.info("[Saga-Step: Debit] ✅ Debit successful. New balance = {}", wallet.getBalance());

            // 6️⃣ Record new balance in context for audit or next steps
            context.put("sourceWalletBalanceAfterDebit", newBalance);

            log.info("[Saga-Step: Debit] ✅ Completed debit step for wallet ID {}", fromWalletId);

            return true;

        } catch (Exception e) {
            log.error("[Saga-Step: Debit] ❌ Failed to debit wallet. Reason: {}", e.getMessage(), e);
            return false; // returning false triggers compensation in saga orchestrator
        }
    }

    /**
     * Compensation (rollback) step:
     *  If the Saga fails later, this undoes the debit operation.
     *
     * Steps:
     *  1️⃣ Fetch wallet with pessimistic lock
     *  2️⃣ Store balance before compensation (for traceability)
     *  3️⃣ Credit back the amount
     *  4️⃣ Save updated wallet
     *  5️⃣ Store post-compensation balance for audit
     */
    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            Long fromWalletId = context.getLong("fromWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (fromWalletId == null || amount == null) {
                throw new IllegalArgumentException("Missing 'fromWalletId' or 'amount' in SagaContext for compensation");
            }

            log.warn("[Saga-Compensation: Debit] 🧾 Reverting ₹{} debit for wallet ID {}", amount, fromWalletId);

            // 1️⃣ Lock the wallet again before modification
            Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                    .orElseThrow(() -> new IllegalStateException("Source wallet not found: ID = " + fromWalletId));

            log.debug("[Saga-Compensation: Debit] Current balance before rollback: {}", wallet.getBalance());

            // 2️⃣ Backup pre-compensation balance for audit
            context.put("sourceWalletBalanceBeforeCreditCompensation", wallet.getBalance());

            // 3️⃣ Undo debit → credit back the same amount
            BigDecimal newBalance = wallet.getBalance().add(amount);

            // 4️⃣ Persist updated wallet
            walletRepository.updateBalance(fromWalletId, newBalance);

            // 5️⃣ Store new balance after rollback for debugging or audit
            context.put("sourceWalletBalanceAfterCreditCompensation", newBalance);

            log.info("[Saga-Compensation: Debit] ✅ Rollback successful. Wallet ID {}, New balance = {}",
                    fromWalletId, wallet.getBalance());

            return true; // Compensation succeeded

        } catch (Exception e) {
            log.error("[Saga-Compensation: Debit] ❌ Failed to rollback debit: {}", e.getMessage(), e);
            return false; // If this fails, manual reconciliation might be needed
        }
    }

    /**
     * Each saga step must have a unique identifier name.
     */
    @Override
    public String getStepName() {
        return SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString();
    }
}
