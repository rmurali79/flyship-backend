package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "shipper_id", nullable = false) private Long shipperId;
    @Column(nullable = false) private String origin;
    @Column(nullable = false) private String destination;
    @Column(columnDefinition = "TEXT") private String details;
    @JsonProperty("item_description") @Column(name = "item_description", columnDefinition = "TEXT") private String itemDescription;
    @JsonProperty("photo_url") @Column(name = "photo_url") private String photoUrl;
    @Column(precision = 10, scale = 2) private BigDecimal weight;
    @JsonProperty("dimension_length") @Column(name = "dimension_length", precision = 10, scale = 2) private BigDecimal dimensionLength;
    @JsonProperty("dimension_width") @Column(name = "dimension_width", precision = 10, scale = 2) private BigDecimal dimensionWidth;
    @JsonProperty("dimension_height") @Column(name = "dimension_height", precision = 10, scale = 2) private BigDecimal dimensionHeight;
    @JsonProperty("max_budget") @Column(name = "max_budget", precision = 10, scale = 2) private BigDecimal maxBudget;
    @JsonProperty("shipment_arrangement") @Enumerated(EnumType.STRING) @Column(name = "shipment_arrangement") private ShipmentArrangement shipmentArrangement;
    @JsonProperty("collection_point") @Column(name = "collection_point") private String collectionPoint;
    @JsonProperty("delivery_recipient_name") @Column(name = "delivery_recipient_name") private String deliveryRecipientName;
    @JsonProperty("delivery_address") @Column(name = "delivery_address", columnDefinition = "TEXT") private String deliveryAddress;
    @JsonProperty("delivery_arrangement") @Enumerated(EnumType.STRING) @Column(name = "delivery_arrangement") private DeliveryArrangement deliveryArrangement;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ShipmentStatus status = ShipmentStatus.pending;
    @JsonProperty("reach_latest_by") @Column(name = "reach_latest_by") private LocalDate reachLatestBy;
    @JsonProperty("escrow_amount") @Column(name = "escrow_amount", precision = 10, scale = 2) private BigDecimal escrowAmount;
    @JsonProperty("escrow_currency") @Column(name = "escrow_currency") private String escrowCurrency = "USD";
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", insertable = false, updatable = false)
    private User shipper;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Shipment() {}

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getShipperId() { return shipperId; } public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public String getOrigin() { return origin; } public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; } public void setDestination(String destination) { this.destination = destination; }
    public String getDetails() { return details; } public void setDetails(String details) { this.details = details; }
    public String getItemDescription() { return itemDescription; } public void setItemDescription(String v) { this.itemDescription = v; }
    public String getPhotoUrl() { return photoUrl; } public void setPhotoUrl(String v) { this.photoUrl = v; }
    public BigDecimal getWeight() { return weight; } public void setWeight(BigDecimal v) { this.weight = v; }
    public BigDecimal getDimensionLength() { return dimensionLength; } public void setDimensionLength(BigDecimal v) { this.dimensionLength = v; }
    public BigDecimal getDimensionWidth() { return dimensionWidth; } public void setDimensionWidth(BigDecimal v) { this.dimensionWidth = v; }
    public BigDecimal getDimensionHeight() { return dimensionHeight; } public void setDimensionHeight(BigDecimal v) { this.dimensionHeight = v; }
    public BigDecimal getMaxBudget() { return maxBudget; } public void setMaxBudget(BigDecimal v) { this.maxBudget = v; }
    public ShipmentArrangement getShipmentArrangement() { return shipmentArrangement; } public void setShipmentArrangement(ShipmentArrangement v) { this.shipmentArrangement = v; }
    public String getCollectionPoint() { return collectionPoint; } public void setCollectionPoint(String v) { this.collectionPoint = v; }
    public String getDeliveryRecipientName() { return deliveryRecipientName; } public void setDeliveryRecipientName(String v) { this.deliveryRecipientName = v; }
    public String getDeliveryAddress() { return deliveryAddress; } public void setDeliveryAddress(String v) { this.deliveryAddress = v; }
    public DeliveryArrangement getDeliveryArrangement() { return deliveryArrangement; } public void setDeliveryArrangement(DeliveryArrangement v) { this.deliveryArrangement = v; }
    public ShipmentStatus getStatus() { return status; } public void setStatus(ShipmentStatus v) { this.status = v; }
    public LocalDate getReachLatestBy() { return reachLatestBy; } public void setReachLatestBy(LocalDate v) { this.reachLatestBy = v; }
    public BigDecimal getEscrowAmount() { return escrowAmount; } public void setEscrowAmount(BigDecimal v) { this.escrowAmount = v; }
    public String getEscrowCurrency() { return escrowCurrency; } public void setEscrowCurrency(String v) { this.escrowCurrency = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    @JsonIgnore public User getShipper() { return shipper; }

    @JsonProperty("cancellation_reason") @Column(name = "cancellation_reason", columnDefinition = "TEXT") private String cancellationReason;
    @JsonProperty("cancellation_reason_category") @Enumerated(EnumType.STRING) @Column(name = "cancellation_reason_category") private DisputeReason cancellationReasonCategory;

    public String getCancellationReason() { return cancellationReason; } public void setCancellationReason(String v) { this.cancellationReason = v; }
    public DisputeReason getCancellationReasonCategory() { return cancellationReasonCategory; } public void setCancellationReasonCategory(DisputeReason v) { this.cancellationReasonCategory = v; }

    public enum ShipmentStatus { pending, accepted, in_transit, delivered, cancelled, deleted }
    public enum ShipmentArrangement { self_handover, need_collection }
    public enum DeliveryArrangement { self_collect, deliver }
}
