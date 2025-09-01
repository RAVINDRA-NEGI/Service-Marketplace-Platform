package com.marketplace.service;

import java.util.List;

import com.marketplace.dto.ReviewDto;
import com.marketplace.model.Booking;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;

public interface ReviewService {
    
    // Create review
    Review createReview(User client, ReviewDto reviewDto);
    
    // Get reviews
    List<Review> getProfessionalReviews(ProfessionalProfile professional);
    List<Review> getClientReviews(User client);
    Review getReviewById(Long id);
    Review getReviewByBooking(Booking booking);
    
    // Update review
    Review updateReview(Long reviewId, User client, ReviewDto reviewDto);
    
    // Delete review
    void deleteReview(Long reviewId, User client);
    
    // Check permissions
    boolean canReviewBooking(User client, Long bookingId);
    boolean hasReviewedProfessional(User client, ProfessionalProfile professional);
    
    // Rating calculations
    Double getAverageRating(ProfessionalProfile professional);
    Long getTotalReviews(ProfessionalProfile professional);
    List<Review> getRecentReviews(ProfessionalProfile professional);
    
    // Get by ID with permission check
    Review getReviewByIdAndClient(Long id, User client);
}