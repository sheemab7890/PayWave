package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.enums.TransactionStatus;
import com.sheemab.shardedsagawallet.enums.TransactionType;
import com.sheemab.shardedsagawallet.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(Long fromWalletId, Long toWalletId, BigDecimal amount, String description) {
        log.info("Creating transaction from wallet {} to wallet {} for amount {} with description {}", fromWalletId, toWalletId, amount, description);

        Transaction transaction = Transaction.builder()
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .type(TransactionType.TRANSFER)
                .description(description)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created with ID: {}", savedTransaction.getId());
        return savedTransaction;
    }

    public Transaction getTransactionById(Long transactionId) {
        log.info("Fetching transaction with ID: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

    }

    public List<Transaction> getTransactionsByWalletId(Long walletId) {
        log.info("Fetching transactions for wallet ID: {}", walletId);
        return transactionRepository.findByWalletId(walletId);
    }

    public List<Transaction> getTransactionsByFromWalletId(Long fromWalletId) {
        log.info("Fetching debit transactions for wallet ID: {}", fromWalletId);
        return transactionRepository.findByFromWalletId(fromWalletId);
    }

    public List<Transaction> getTransactionsByToWalletId(Long toWalletId) {
        log.info("Fetching credit transactions for wallet ID: {}", toWalletId);
        return transactionRepository.findByToWalletId(toWalletId);
    }

    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        log.info("Fetching transactions with status: {}", status);
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> getTransactionsBySagaInstanceId(Long sagaInstanceId) {
        log.info("Fetching transactions for saga instance ID: {}", sagaInstanceId);
        return transactionRepository.findBySagaInstanceId(sagaInstanceId);
    }

    public void updateTransactionWithSagaInstanceId(Long transactionId, Long sagaInstanceId) {
        log.info("Updating transaction ID {} with saga instance ID {}", transactionId, sagaInstanceId);
        Transaction transaction = getTransactionById(transactionId);
        transaction.setSagaInstanceId(sagaInstanceId);
        transactionRepository.save(transaction);
        log.info("Transaction ID {} updated with saga instance ID {}", transactionId, sagaInstanceId);
    }
}
