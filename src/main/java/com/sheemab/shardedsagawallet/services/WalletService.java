package com.sheemab.shardedsagawallet.services;

import com.sheemab.shardedsagawallet.entities.Wallet;
import com.sheemab.shardedsagawallet.repositories.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public Wallet creatdWallet(Long userId) {
        log.info("Creating wallet for user ID: {}", userId);

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .build();

       wallet = walletRepository.save(wallet);
        log.info("Wallet created with ID: {}", wallet.getId());
        return wallet;
    }

    public Wallet getWalletById(Long id) {
        log.info("Fetching wallet with ID: {}", id);
        return walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with ID: " + id));
    }

    public Wallet getWalletByUserId(Long userId) {
        log.info("Fetching wallet for user ID: {}", userId);
        return walletRepository.findByUserId(userId).get(0);
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        log.info("Fetching wallets for user ID: {}", userId);
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        log.info("Debiting ₹{} from wallet ID: {}", amount, userId);
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        walletRepository.updateBalance(userId, newBalance);
        // Refresh the entity to get latest balance
        entityManager.refresh(wallet);
        log.info("Debited ₹{} from user wallet ID: {}. New balance: ₹{}", amount, userId, wallet.getBalance());
    }

    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        log.info("Crediting ₹{} to wallet ID: {}", amount, userId);
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal newBalance = wallet.getBalance().add(amount);
        walletRepository.updateBalance(userId, newBalance);  // Update balance directly in the repository instead  walletRepository.save(wallet);
        // Refresh the entity to get latest balance
        entityManager.refresh(wallet);
        log.info("Credited ₹{} to wallet ID: {}. New balance: ₹{}", amount, userId, wallet.getBalance());

    }

    public BigDecimal getWalletBalance(Long walletId) {
        log.info("Fetching balance for wallet ID: {}", walletId);
        Wallet wallet = getWalletById(walletId);
        log.info("Wallet ID: {} has balance: ₹{}", walletId, wallet.getBalance());
        return wallet.getBalance();
    }

}
