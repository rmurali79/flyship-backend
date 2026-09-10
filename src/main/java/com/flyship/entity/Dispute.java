package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("subject_type") @Enumerated(EnumType.STRING) @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @JsonProperty("subject_id") @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @JsonProperty("filed_by_user_id") @Column(name = "filed_by_user_id", nullable = false)
    private Long filedByUserId;

    @JsonProperty("respondent_user_id") @Column(name = "respondent_user_id", nullable = false)
    private Long respondentUserId;

    @JsonProperty("reason_category") @Enumerated(EnumType.STRING) @Column(name = "reason_category", nullable = false)
    private DisputeReason reasonCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private DisputeStatus status = DisputeStatus.open;

    @JsonProperty("resolution_notes") @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @JsonProperty("resolved_by_user_id") @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @JsonProperty("resolved_at") @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Dispute() {}

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public SubjectType getSubjectType() { return subjectType; } public void setSubjectType(SubjectType v) { this.subjectType = v; }
    public Long getSubjectId() { return subjectId; } public void setSubjectId(Long v) { this.subjectId = v; }
    public Long getFiledByUserId() { return filedByUserId; } public void setFiledByUserId(Long v) { this.filedByUserId = v; }
    public Long getRespondentUserId() { return respondentUserId; } public void setRespondentUserId(Long v) { this.respondentUserId = v; }
    public DisputeReason getReasonCategory() { return reasonCategory; } public void setReasonCategory(DisputeReason v) { this.reasonCategory = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public DisputeStatus getStatus() { return status; } public void setStatus(DisputeStatus v) { this.status = v; }
    public String getResolutionNotes() { return resolutionNotes; } public void setResolutionNotes(String v) { this.resolutionNotes = v; }
    public Long getResolvedByUserId() { return resolvedByUserId; } public void setResolvedByUserId(Long v) { this.resolvedByUserId = v; }
    public LocalDateTime getResolvedAt() { return resolvedAt; } public void setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum SubjectType { shipment, quote }
    public enum DisputeStatus { open, under_review, accepted, rejected, withdrawn }
}
