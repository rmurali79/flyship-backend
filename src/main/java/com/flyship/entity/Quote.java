package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
public class Quote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonProperty("shipment_id") @Column(name = "shipment_id") private Long shipmentId;
    @JsonProperty("traveler_id") @Column(name = "traveler_id") private Long travelerId;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(length = 3) private String currency = "USD";
    @Column(columnDefinition = "TEXT") private String message;
    @JsonProperty("delivery_date") @Column(name = "delivery_date") private LocalDate deliveryDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QuoteStatus status = QuoteStatus.pending;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "traveler_id", insertable = false, updatable = false) private User traveler;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shipment_id", insertable = false, updatable = false) private Shipment shipment;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Quote() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public Long getTravelerId() { return travelerId; } public void setTravelerId(Long v) { this.travelerId = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public String getCurrency() { return currency; } public void setCurrency(String v) { this.currency = v; }
    public String getMessage() { return message; } public void setMessage(String v) { this.message = v; }
    public LocalDate getDeliveryDate() { return deliveryDate; } public void setDeliveryDate(LocalDate v) { this.deliveryDate = v; }
    public QuoteStatus getStatus() { return status; } public void setStatus(QuoteStatus v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    @JsonIgnore public User getTraveler() { return traveler; }
    @JsonIgnore public Shipment getShipment() { return shipment; }

    @JsonProperty("withdrawal_reason") @Column(name = "withdrawal_reason", columnDefinition = "TEXT") private String withdrawalReason;

    public String getWithdrawalReason() { return withdrawalReason; } public void setWithdrawalReason(String v) { this.withdrawalReason = v; }

    public enum QuoteStatus { pending, accepted, rejected, withdrawn }
}
