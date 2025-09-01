package com.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Booking;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByProfessionalOrderByCreatedAtDesc(ProfessionalProfile professional);
    
    List<Review> findByClientOrderByCreatedAtDesc(User client);
    
    Optional<Review> findByBooking(Booking booking);
    
    Optional<Review> findByProfessionalAndClient(ProfessionalProfile professional, User client);
    
    boolean existsByProfessionalAndClient(ProfessionalProfile professional, User client);
    
    boolean existsByBooking(Booking booking);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.professional = :professional")
    Double findAverageRatingByProfessional(@Param("professional") ProfessionalProfile professional);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.professional = :professional")
    Long countReviewsByProfessional(@Param("professional") ProfessionalProfile professional);
    
    @Query("SELECT r FROM Review r WHERE r.professional = :professional ORDER BY r.createdAt DESC LIMIT 10")
    List<Review> findRecentReviewsByProfessional(@Param("professional") ProfessionalProfile professional);
}