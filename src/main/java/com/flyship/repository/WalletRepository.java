package com.flyship.repository;

import com.flyship.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUserIdAndCurrency(Long userId, String currency);
}
