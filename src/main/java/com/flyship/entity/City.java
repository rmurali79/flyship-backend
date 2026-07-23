package com.flyship.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cities")
public class City {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, unique = true, length = 10) private String code;
    @Column(nullable = false, length = 100) private String country;
    @JsonProperty("image_url") @Column(name = "image_url") private String imageUrl;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public City() {}
    public City(String name, String code, String country) {
        this.name = name; this.code = code; this.country = country;
    }
    public City(String name, String code, String country, String imageUrl) {
        this.name = name; this.code = code; this.country = country; this.imageUrl = imageUrl;
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getCode() { return code; } public void setCode(String v) { this.code = v; }
    public String getCountry() { return country; } public void setCountry(String v) { this.country = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { this.imageUrl = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
