package com.sheemab.shardedsagawallet.controller;

import com.sheemab.shardedsagawallet.dtos.CreateWalletRequestDto;
import com.sheemab.shardedsagawallet.dtos.CreditWalletRequestDto;
import com.sheemab.shardedsagawallet.dtos.DebitWalletRequestDto;
import com.sheemab.shardedsagawallet.entities.Wallet;
import com.sheemab.shardedsagawallet.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequestDto requestDto) {
        Wallet wallet = walletService.createWallet(requestDto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long walletId) {
        Wallet wallet = walletService.getWalletById(walletId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BigDecimal> getWalletBalance(@RequestParam Long walletId) {
        BigDecimal balance = walletService.getWalletBalance(walletId);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{userId}/debit" )
    public ResponseEntity<Wallet> debitWallet(@PathVariable Long userId, @RequestBody DebitWalletRequestDto request) {
        walletService.debit(userId, request.getAmount());
        Wallet updatedWallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(updatedWallet);
    }

    @PostMapping("/{userId}/credit" )
    public ResponseEntity<Wallet> creditWallet(@PathVariable Long userId, @RequestBody CreditWalletRequestDto request) {
        walletService.credit(userId, request.getAmount());
        Wallet updatedWallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(updatedWallet);
    }
}
