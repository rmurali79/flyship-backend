package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"shipment_id", "reviewer_id"}))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonProperty("shipment_id") @Column(name = "shipment_id", nullable = false) private Long shipmentId;
    @JsonProperty("reviewer_id") @Column(name = "reviewer_id", nullable = false) private Long reviewerId;
    @JsonProperty("reviewee_id") @Column(name = "reviewee_id", nullable = false) private Long revieweeId;
    @Column(nullable = false) private Integer rating;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id", insertable = false, updatable = false) private User reviewer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewee_id", insertable = false, updatable = false) private User reviewee;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Review() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long v) { this.shipmentId = v; }
    public Long getReviewerId() { return reviewerId; } public void setReviewerId(Long v) { this.reviewerId = v; }
    public Long getRevieweeId() { return revieweeId; } public void setRevieweeId(Long v) { this.revieweeId = v; }
    public Integer getRating() { return rating; } public void setRating(Integer v) { this.rating = v; }
    public String getComment() { return comment; } public void setComment(String v) { this.comment = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    @JsonIgnore public User getReviewer() { return reviewer; }
    @JsonIgnore public User getReviewee() { return reviewee; }
}
