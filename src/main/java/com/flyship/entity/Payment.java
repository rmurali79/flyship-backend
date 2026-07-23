package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "shipment_id") private Long shipmentId;
    @Column(name = "quote_id") private Long quoteId;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(length = 50) private String status = "completed";
    @Column(name = "stripe_payment_id") private String transactionId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", insertable = false, updatable = false) private User user;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shipment_id", insertable = false, updatable = false) private Shipment shipment;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quote_id", insertable = false, updatable = false) private Quote quote;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Payment() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public Long getQuoteId() { return quoteId; } public void setQuoteId(Long v) { this.quoteId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public String getTransactionId() { return transactionId; } public void setTransactionId(String v) { this.transactionId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
