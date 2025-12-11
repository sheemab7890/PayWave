package com.sheemab.shardedsagawallet.services.saga.steps;

import com.sheemab.shardedsagawallet.entities.Wallet;
import com.sheemab.shardedsagawallet.enums.SagaStepType;
import com.sheemab.shardedsagawallet.repositories.WalletRepository;
import com.sheemab.shardedsagawallet.services.saga.SagaContext;
import com.sheemab.shardedsagawallet.services.saga.SagaStepInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**

 * ✅ Purpose:
 * Saga step responsible for CREDITING the destination wallet
 * during a distributed money transfer between wallets.

 * 🔁 Saga Workflow:
 *  1️⃣ execute()     → Perform the forward action (crediting the wallet)
 *  2️⃣ compensate()  → Rollback action if any later step fails (undo credit)

 * 🧠 Key Idea:
 *  Uses PESSIMISTIC LOCKING to prevent concurrent balance updates.
 *  Uses SagaContext to store temporary data for rollback/recovery.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditDestinationWalletStep implements SagaStepInterface {

    private final WalletRepository walletRepository;

    /**
     * Executes the saga step:
     * - Fetch the destination wallet with lock
     * - Credit the balance
     * - Save updated state
     * - Record values in SagaContext for rollback tracking
     */
    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        // Wrap the whole operation safely
        try {
            // 1️⃣ Extract input data from the shared SagaContext
            Long toWalletId = context.getLong("toWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (toWalletId == null || amount == null) {
                throw new IllegalArgumentException("Missing 'toWalletId' or 'amount' in SagaContext");
            }

            log.info("[Saga-Step: Credit] Initiating credit of ₹{} to wallet ID {}", amount, toWalletId);

            // 2️⃣ Fetch wallet record with pessimistic write lock
            // This ensures no other transaction can modify it at the same time
            Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                    .orElseThrow(() -> new IllegalStateException("Destination wallet not found with ID: " + toWalletId));

            log.debug("[Saga-Step: Credit] Wallet current balance = {}", wallet.getBalance());

            // 3️⃣ Keep backup of original balance for compensation (rollback)
            context.put("originalToWalletBalance", wallet.getBalance());

            // 4️⃣ Perform the credit operation (only modifies object)
              BigDecimal newBalance = wallet.getBalance().add(amount);

            // 5️⃣ Persist updated balance in DB
            walletRepository.updateBalance(toWalletId, newBalance);
            log.info("[Saga-Step: Credit] Wallet credited successfully. New balance = {}", wallet.getBalance());

            // 6️⃣ Store post-credit balance for next step / audit
            context.put("toWalletBalanceAfterCredit", wallet.getBalance());

            log.info("[Saga-Step: Credit] ✅ Completed credit step for wallet ID {}", toWalletId);
            return true;

        } catch (Exception e) {
            // Log detailed reason and mark step as failed
            log.error("[Saga-Step: Credit] ❌ Failed to credit wallet. Reason: {}", e.getMessage(), e);
            return false; // triggers compensation flow in orchestrator
        }
    }


    /**
     * Compensation step (rollback):
     * If this saga step succeeded but a later step fails,
     * this method reverses the previous credit operation by debiting back the same amount.
     */
    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        try {
            // 1️⃣ Extract required rollback info
            Long toWalletId = context.getLong("toWalletId");
            BigDecimal amount = context.getBigDecimal("amount");

            if (toWalletId == null || amount == null) {
                throw new IllegalArgumentException("Missing 'toWalletId' or 'amount' in SagaContext for compensation");
            }

            log.warn("[Saga-Compensation: Credit] Reverting ₹{} credit for wallet ID {}", amount, toWalletId);

            // 2️⃣ Lock wallet record again to ensure safe concurrent access
            Wallet wallet = walletRepository.findByIdWithLock(toWalletId)
                    .orElseThrow(() -> new IllegalStateException("Destination wallet not found with ID: " + toWalletId));

            // 3️⃣ Perform debit (undo credit)
            BigDecimal newBalance = wallet.getBalance().subtract(amount);
            walletRepository.updateBalance(toWalletId, newBalance);

            // 4️⃣ Record rollback info for debugging/audit
            context.put("toWalletBalanceAfterCreditCompensation", wallet.getBalance());

            log.info("[Saga-Compensation: Credit] ✅ Rollback successful. Wallet ID {}, New balance = {}",
                    toWalletId, wallet.getBalance());
            return true;

        } catch (Exception e) {
            log.error("[Saga-Compensation: Credit] ❌ Rollback failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Each saga step should have a unique name for orchestration and logging.
     */
    @Override
    public String getStepName() {
        return SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString() ;
    }
}
