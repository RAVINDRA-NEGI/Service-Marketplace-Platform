// Updated ReviewRepository.java
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

    // Find methods with soft delete support
    @Query("SELECT r FROM Review r JOIN FETCH r.client JOIN FETCH r.booking JOIN FETCH r.professional " +
           "WHERE r.professional = :professional AND r.deleted = false " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByProfessionalAndDeletedFalseOrderByCreatedAtDesc(@Param("professional") ProfessionalProfile professional);

    @Query("SELECT r FROM Review r JOIN FETCH r.professional JOIN FETCH r.booking " +
           "WHERE r.client = :client AND r.deleted = false " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByClientAndDeletedFalseOrderByCreatedAtDesc(@Param("client") User client);

    Optional<Review> findByIdAndDeletedFalse(Long id);

    Optional<Review> findByBookingAndDeletedFalse(Booking booking);

    // Existence checks with soft delete support
    boolean existsByBookingAndDeletedFalse(Booking booking);

    boolean existsByProfessionalAndClientAndDeletedFalse(ProfessionalProfile professional, User client);

    // Rating calculations with soft delete support
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.professional = :professional AND r.deleted = false")
    Double findAverageRatingByProfessionalAndDeletedFalse(@Param("professional") ProfessionalProfile professional);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.professional = :professional AND r.deleted = false")
    Long countReviewsByProfessionalAndDeletedFalse(@Param("professional") ProfessionalProfile professional);

    // Recent reviews (last 10) with soft delete support
    @Query("SELECT r FROM Review r JOIN FETCH r.client JOIN FETCH r.booking " +
           "WHERE r.professional = :professional AND r.deleted = false " +
           "ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByProfessionalAndDeletedFalse(@Param("professional") ProfessionalProfile professional);

    // Rating distribution for analytics
    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
           "WHERE r.professional = :professional AND r.deleted = false " +
           "GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> findRatingDistributionByProfessional(@Param("professional") ProfessionalProfile professional);

    // Reviews within time range
    @Query("SELECT r FROM Review r JOIN FETCH r.client JOIN FETCH r.booking " +
           "WHERE r.professional = :professional AND r.deleted = false " +
           "AND r.createdAt >= :startDate AND r.createdAt <= :endDate " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByProfessionalAndDateRangeAndDeletedFalse(
        @Param("professional") ProfessionalProfile professional,
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate);

    // Reviews by rating range
    @Query("SELECT r FROM Review r JOIN FETCH r.client JOIN FETCH r.booking " +
           "WHERE r.professional = :professional AND r.deleted = false " +
           "AND r.rating >= :minRating AND r.rating <= :maxRating " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByProfessionalAndRatingRangeAndDeletedFalse(
        @Param("professional") ProfessionalProfile professional,
        @Param("minRating") Integer minRating,
        @Param("maxRating") Integer maxRating);

    // For admin purposes - find all reviews including deleted ones
    @Query("SELECT r FROM Review r JOIN FETCH r.client JOIN FETCH r.professional JOIN FETCH r.booking " +
           "WHERE r.professional = :professional ORDER BY r.createdAt DESC")
    List<Review> findAllByProfessionalIncludingDeleted(@Param("professional") ProfessionalProfile professional);
}