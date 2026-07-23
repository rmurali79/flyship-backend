package com.flyship.repository;

import com.flyship.entity.WalletLock;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.entity.WalletLock.LockType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletLockRepository extends JpaRepository<WalletLock, Long> {
    Optional<WalletLock> findByReferenceIdAndTypeAndStatus(Long referenceId, LockType type, LockStatus status);
    Optional<WalletLock> findByWalletIdAndTypeAndReferenceIdAndStatus(
            Long walletId, LockType type, Long referenceId, LockStatus status);
}
