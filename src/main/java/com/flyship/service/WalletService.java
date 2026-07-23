package com.flyship.service;

import com.flyship.entity.Wallet;
import com.flyship.entity.WalletLock;
import com.flyship.entity.WalletTransaction;
import com.flyship.repository.WalletLockRepository;
import com.flyship.repository.WalletRepository;
import com.flyship.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletLockRepository walletLockRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;

    public WalletLock lockFunds(Long userId, String currency, BigDecimal amount,
                                 WalletLock.LockType type, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUserId(userId); w.setCurrency(currency);
                    w.setBalance(new BigDecimal("1000.00")); w.setLockedBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance to lock funds");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);

        WalletLock lock = new WalletLock();
        lock.setWalletId(wallet.getId()); lock.setAmount(amount);
        lock.setType(type); lock.setReferenceId(referenceId);
        lock.setStatus(WalletLock.LockStatus.active);
        return walletLockRepository.save(lock);
    }

    public void unlockFunds(Long userId, String currency, WalletLock.LockType type, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Optional<WalletLock> lockOpt = walletLockRepository
                .findByWalletIdAndTypeAndReferenceIdAndStatus(wallet.getId(), type, referenceId, WalletLock.LockStatus.active);
        if (lockOpt.isEmpty()) return;

        WalletLock lock = lockOpt.get();
        wallet.setBalance(wallet.getBalance().add(lock.getAmount()));
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(lock.getAmount()));
        walletRepository.save(wallet);

        lock.setStatus(WalletLock.LockStatus.released);
        walletLockRepository.save(lock);
    }

    public void consumeLock(Long userId, String currency, WalletLock.LockType type, Long referenceId) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        WalletLock lock = walletLockRepository
                .findByWalletIdAndTypeAndReferenceIdAndStatus(wallet.getId(), type, referenceId, WalletLock.LockStatus.active)
                .orElseThrow(() -> new RuntimeException("Active lock not found"));

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(lock.getAmount()));
        walletRepository.save(wallet);
        lock.setStatus(WalletLock.LockStatus.consumed);
        walletLockRepository.save(lock);
    }

    public Map<String, BigDecimal> applyCancellationPenalty(WalletLock lock, Long victimUserId) {
        if (lock.getStatus() != WalletLock.LockStatus.active) throw new RuntimeException("Lock is not active");

        Wallet wallet = walletRepository.findById(lock.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        String currency = wallet.getCurrency();
        BigDecimal totalAmount = lock.getAmount();
        BigDecimal penaltyAmount = totalAmount.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal feeAmount = totalAmount.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = totalAmount.subtract(penaltyAmount).subtract(feeAmount);

        wallet.setLockedBalance(wallet.getLockedBalance().subtract(totalAmount));
        wallet.setBalance(wallet.getBalance().add(refundAmount));
        walletRepository.save(wallet);

        Wallet victimWallet = walletRepository.findByUserIdAndCurrency(victimUserId, currency)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUserId(victimUserId); w.setCurrency(currency);
                    w.setBalance(BigDecimal.ZERO); w.setLockedBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
        victimWallet.setBalance(victimWallet.getBalance().add(penaltyAmount));
        walletRepository.save(victimWallet);

        WalletTransaction victimTx = new WalletTransaction();
        victimTx.setWalletId(victimWallet.getId());
        victimTx.setType(WalletTransaction.TransactionType.penalty);
        victimTx.setAmount(penaltyAmount);
        victimTx.setStatus(WalletTransaction.TransactionStatus.completed);
        victimTx.setReference(String.format("Compensation from canceled transaction (Lock #%d)", lock.getId()));
        walletTransactionRepository.save(victimTx);

        lock.setStatus(WalletLock.LockStatus.consumed);
        walletLockRepository.save(lock);

        BigDecimal deduction = penaltyAmount.add(feeAmount);
        WalletTransaction ownerTx = new WalletTransaction();
        ownerTx.setWalletId(wallet.getId());
        ownerTx.setType(WalletTransaction.TransactionType.penalty);
        ownerTx.setAmount(deduction.negate());
        ownerTx.setStatus(WalletTransaction.TransactionStatus.completed);
        ownerTx.setReference(String.format("Cancellation penalty deducted (Lock #%d)", lock.getId()));
        walletTransactionRepository.save(ownerTx);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("refundAmount", refundAmount);
        result.put("penaltyAmount", penaltyAmount);
        result.put("feeAmount", feeAmount);
        return result;
    }
}
