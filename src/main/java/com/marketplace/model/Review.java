package com.marketplace.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    
    public static final int MAX_COMMENT_LENGTH = 1000;
    public static final int MIN_COMMENT_LENGTH = 10;
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    @ToString.Exclude
    private ProfessionalProfile professional;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    private User client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    private Booking booking;
    
    @Min(MIN_RATING)
    @Max(MAX_RATING)
    @Column(nullable = false)
    private Integer rating;
    
    @NotBlank
    @Size(min = MIN_COMMENT_LENGTH, max = MAX_COMMENT_LENGTH)
    @Column(length = MAX_COMMENT_LENGTH, nullable = false)
    private String comment;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // For optimistic locking
    @Version
    private Long version;
    
    // Soft delete flag
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (deleted == null) {
            deleted = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructor for creating new review
    public Review(ProfessionalProfile professional, User client, Booking booking, Integer rating, String comment) {
        this.professional = professional;
        this.client = client;
        this.booking = booking;
        this.rating = rating;
        this.comment = comment;
        this.deleted = false;
    }
    
    // Business methods
    public boolean isDeleted() {
        return deleted != null && deleted;
    }
    
    public void markAsDeleted() {
        this.deleted = true;
    }
    
    public boolean canBeEditedBy(User user) {
        return user != null && user.getId().equals(this.client.getId()) && !isDeleted();
    }
}