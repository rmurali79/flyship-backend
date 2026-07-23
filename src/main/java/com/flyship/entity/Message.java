package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonProperty("shipment_id") @Column(name = "shipment_id", nullable = false) private Long shipmentId;
    @JsonProperty("sender_id") @Column(name = "sender_id", nullable = false) private Long senderId;
    @JsonProperty("receiver_id") @Column(name = "receiver_id", nullable = false) private Long receiverId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @JsonProperty("message_type") @Column(name = "message_type", length = 20) private String messageType = "text";
    @Column(name = "is_read") private Boolean isRead = false;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Message() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public Long getSenderId() { return senderId; } public void setSenderId(Long v) { this.senderId = v; }
    public Long getReceiverId() { return receiverId; } public void setReceiverId(Long v) { this.receiverId = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public String getMessageType() { return messageType; } public void setMessageType(String v) { this.messageType = v; }
    public Boolean getIsRead() { return isRead; } public void setIsRead(Boolean v) { this.isRead = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
