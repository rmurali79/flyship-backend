package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_plans")
public class TravelPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "traveler_id") private Long travelerId;
    @Column(nullable = false, length = 100) private String origin;
    @Column(nullable = false, length = 100) private String destination;
    @JsonProperty("start_date") @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @JsonProperty("end_date") @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @JsonProperty("available_baggage_kg") @Column(name = "available_baggage_kg", precision = 10, scale = 2) private BigDecimal availableBaggageKg;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "traveler_id", insertable = false, updatable = false) private User traveler;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public TravelPlan() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getTravelerId() { return travelerId; } public void setTravelerId(Long v) { this.travelerId = v; }
    public String getOrigin() { return origin; } public void setOrigin(String v) { this.origin = v; }
    public String getDestination() { return destination; } public void setDestination(String v) { this.destination = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { this.endDate = v; }
    public BigDecimal getAvailableBaggageKg() { return availableBaggageKg; } public void setAvailableBaggageKg(BigDecimal v) { this.availableBaggageKg = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
