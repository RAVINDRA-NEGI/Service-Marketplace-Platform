package com.marketplace.model;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "professional_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotBlank
    @Column(length = 1000)
    private String bio;

    private String certification;

    private String profilePhotoUrl;
    

    @Column(name = "certificates_path")
    private String certificatesPath;

    @Column(name = "certificates_urls")
    private String certificatesUrls; 

    @Column(name = "profile_photo_path")
    private String profilePhotoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Positive
    private Double hourlyRate;

    private String serviceAreaCity;

    private String serviceAreaState;

    private Double averageRating = 0.0;

    private Integer totalReviews = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Availability> availability = new ArrayList<>();

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ProfessionalProfile(User user, String bio, ServiceCategory category) {
        this.user = user;
        this.bio = bio;
        this.category = category;
    }
}