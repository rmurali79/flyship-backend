package com.flyship.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "wallet_id", nullable = false) private Long walletId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionType type;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) private TransactionStatus status = TransactionStatus.pending;
    private String reference;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "wallet_id", insertable = false, updatable = false) private Wallet wallet;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public WalletTransaction() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getWalletId() { return walletId; } public void setWalletId(Long v) { this.walletId = v; }
    public TransactionType getType() { return type; } public void setType(TransactionType v) { this.type = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public TransactionStatus getStatus() { return status; } public void setStatus(TransactionStatus v) { this.status = v; }
    public String getReference() { return reference; } public void setReference(String v) { this.reference = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum TransactionType { deposit, withdrawal, transfer, penalty }
    public enum TransactionStatus { pending, completed, failed }
}
