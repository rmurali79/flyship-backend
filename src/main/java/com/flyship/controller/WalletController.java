package com.flyship.controller;

import com.flyship.entity.Wallet;
import com.flyship.entity.WalletTransaction;
import com.flyship.repository.WalletRepository;
import com.flyship.repository.WalletTransactionRepository;
import com.flyship.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;

    @GetMapping
    public ResponseEntity<?> getWallets(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(walletRepository.findByUserId(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    @Transactional
    public ResponseEntity<?> deposit(@RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String currency = body.get("currency");
            BigDecimal amount = new BigDecimal(body.get("amount"));

            Wallet wallet = walletRepository.findByUserIdAndCurrency(user.getId(), currency)
                    .orElseGet(() -> {
                        Wallet w = new Wallet();
                        w.setUserId(user.getId()); w.setCurrency(currency);
                        w.setBalance(BigDecimal.ZERO); w.setLockedBalance(BigDecimal.ZERO);
                        return walletRepository.save(w);
                    });

            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWalletId(wallet.getId());
            tx.setType(WalletTransaction.TransactionType.deposit);
            tx.setAmount(amount);
            tx.setStatus(WalletTransaction.TransactionStatus.completed);
            tx.setReference("Manual Deposit");
            walletTransactionRepository.save(tx);

            return ResponseEntity.ok(Map.of("message", "Deposit successful", "wallet", wallet));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    @Transactional
    public ResponseEntity<?> withdraw(@RequestBody Map<String, String> body,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String currency = body.get("currency");
            BigDecimal amount = new BigDecimal(body.get("amount"));

            Wallet wallet = walletRepository.findByUserIdAndCurrency(user.getId(), currency)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            if (wallet.getBalance().compareTo(amount) < 0)
                return ResponseEntity.badRequest().body(Map.of("error", "Insufficient funds"));

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWalletId(wallet.getId());
            tx.setType(WalletTransaction.TransactionType.withdrawal);
            tx.setAmount(amount);
            tx.setStatus(WalletTransaction.TransactionStatus.completed);
            tx.setReference("Withdrawal to Bank");
            walletTransactionRepository.save(tx);

            return ResponseEntity.ok(Map.of("message", "Withdrawal successful", "wallet", wallet));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
