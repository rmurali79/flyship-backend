package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency"}))
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 3) private String currency;
    @Column(precision = 10, scale = 2) private BigDecimal balance = BigDecimal.ZERO;
    @Column(name = "locked_balance", precision = 10, scale = 2) private BigDecimal lockedBalance = BigDecimal.ZERO;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", insertable = false, updatable = false) private User user;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Wallet() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public String getCurrency() { return currency; } public void setCurrency(String v) { this.currency = v; }
    public BigDecimal getBalance() { return balance; } public void setBalance(BigDecimal v) { this.balance = v; }
    public BigDecimal getLockedBalance() { return lockedBalance; } public void setLockedBalance(BigDecimal v) { this.lockedBalance = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
