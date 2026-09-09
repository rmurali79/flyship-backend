package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("user_id") @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String message;
    @JsonProperty("shipment_id") @Column(name = "shipment_id") private Long shipmentId;
    @Column(nullable = false) private boolean read = false;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Notification() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public NotificationType getType() { return type; } public void setType(NotificationType v) { this.type = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getMessage() { return message; } public void setMessage(String v) { this.message = v; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public boolean isRead() { return read; } public void setRead(boolean v) { this.read = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum NotificationType { new_quote, quote_accepted, shipment_status_change }
}
