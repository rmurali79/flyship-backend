package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_evidence")
public class DisputeEvidence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("dispute_id") @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @JsonProperty("photo_url") @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @JsonProperty("uploaded_by_user_id") @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public DisputeEvidence() {}

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getDisputeId() { return disputeId; } public void setDisputeId(Long v) { this.disputeId = v; }
    public String getPhotoUrl() { return photoUrl; } public void setPhotoUrl(String v) { this.photoUrl = v; }
    public Long getUploadedByUserId() { return uploadedByUserId; } public void setUploadedByUserId(Long v) { this.uploadedByUserId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
