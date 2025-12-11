package com.sheemab.shardedsagawallet.repositories;

import com.sheemab.shardedsagawallet.entities.Transaction;
import com.sheemab.shardedsagawallet.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromWalletId(Long fromWalletId); //all the debit transactions

    List<Transaction> findByToWalletId(Long toWalletId); // all the credit transactions

    @Query("SELECT t FROM Transaction t WHERE t.fromWalletId = :walletId OR t.toWalletId = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId); // all the transactions related to a wallet

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findBySagaInstanceId(Long sagaInstanceId);
}
