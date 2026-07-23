package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_histories")
public class ShipmentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "shipment_id") private Long shipmentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Shipment.ShipmentStatus status;
    @Column(columnDefinition = "TEXT") private String description;
    private String location;
    @Column(name = "timestamp") private LocalDateTime timestamp;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shipment_id", insertable = false, updatable = false) private Shipment shipment;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); if (timestamp == null) timestamp = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public ShipmentHistory() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public Shipment.ShipmentStatus getStatus() { return status; } public void setStatus(Shipment.ShipmentStatus v) { this.status = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getLocation() { return location; } public void setLocation(String v) { this.location = v; }
    public LocalDateTime getTimestamp() { return timestamp; } public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
