package com.flyship.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_locks")
public class WalletLock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "wallet_id", nullable = false) private Long walletId;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LockType type;
    @Column(name = "reference_id", nullable = false) private Long referenceId;
    @Enumerated(EnumType.STRING) private LockStatus status = LockStatus.active;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "wallet_id", insertable = false, updatable = false) private Wallet wallet;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public WalletLock() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getWalletId() { return walletId; } public void setWalletId(Long v) { this.walletId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public LockType getType() { return type; } public void setType(LockType v) { this.type = v; }
    public Long getReferenceId() { return referenceId; } public void setReferenceId(Long v) { this.referenceId = v; }
    public LockStatus getStatus() { return status; } public void setStatus(LockStatus v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Wallet getWallet() { return wallet; }

    public enum LockType { shipment_budget, quote_collateral }
    public enum LockStatus { active, released, consumed }
}
