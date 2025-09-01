package com.marketplace.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.ReviewDto;
import com.marketplace.enums.BookingStatus;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.Booking;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;
import com.marketplace.repository.BookingRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.ReviewRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                           BookingRepository bookingRepository,
                           ProfessionalProfileRepository profileRepository,
                           UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Review createReview(User client, ReviewDto reviewDto) {
        logger.info("Creating review for client ID: {} and professional ID: {}", client.getId(), reviewDto.getProfessionalId());

        // Get booking
        Booking booking = bookingRepository.findById(reviewDto.getBookingId())
                .orElseThrow(() -> new UsernameTakenException("Booking not found"));

        // Verify booking belongs to client and is completed
        if (!booking.getClient().getId().equals(client.getId())) {
            throw new UsernameTakenException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new UsernameTakenException("You can only review completed bookings");
        }

        // Check if already reviewed
        if (reviewRepository.existsByBooking(booking)) {
            throw new UsernameTakenException("You have already reviewed this booking");
        }

        // Get professional
        ProfessionalProfile professional = profileRepository.findById(reviewDto.getProfessionalId())
                .orElseThrow(() -> new UsernameTakenException("Professional not found"));

        // Create review
        Review review = new Review(professional, client, booking, reviewDto.getRating(), reviewDto.getComment());
        Review savedReview = reviewRepository.save(review);

        // Update professional's average rating
        updateProfessionalRating(professional);

        logger.info("Review created successfully with ID: {}", savedReview.getId());
        return savedReview;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getProfessionalReviews(ProfessionalProfile professional) {
        return reviewRepository.findByProfessionalOrderByCreatedAtDesc(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getClientReviews(User client) {
        return reviewRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new UsernameTakenException("Review not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewByBooking(Booking booking) {
        return reviewRepository.findByBooking(booking)
                .orElseThrow(() -> new UsernameTakenException("Review not found for this booking"));
    }

    @Override
    @Transactional
    public Review updateReview(Long reviewId, User client, ReviewDto reviewDto) {
        Review review = getReviewByIdAndClient(reviewId, client);
        
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        
        Review updatedReview = reviewRepository.save(review);
        
        // Update professional's average rating
        updateProfessionalRating(review.getProfessional());
        
        return updatedReview;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, User client) {
        Review review = getReviewByIdAndClient(reviewId, client);
        ProfessionalProfile professional = review.getProfessional();
        
        reviewRepository.delete(review);
        
        // Update professional's average rating
        updateProfessionalRating(professional);
        
        logger.info("Review deleted successfully with ID: {}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReviewBooking(User client, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new UsernameTakenException("Booking not found"));

        // Check if booking belongs to client
        if (!booking.getClient().getId().equals(client.getId())) {
            return false;
        }

        // Check if booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }

        // Check if already reviewed
        return !reviewRepository.existsByBooking(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReviewedProfessional(User client, ProfessionalProfile professional) {
        return reviewRepository.existsByProfessionalAndClient(professional, client);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(ProfessionalProfile professional) {
        return reviewRepository.findAverageRatingByProfessional(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalReviews(ProfessionalProfile professional) {
        return reviewRepository.countReviewsByProfessional(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getRecentReviews(ProfessionalProfile professional) {
        return reviewRepository.findRecentReviewsByProfessional(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewByIdAndClient(Long id, User client) {
        Review review = getReviewById(id);
        
        if (!review.getClient().getId().equals(client.getId())) {
            throw new UsernameTakenException("Access denied");
        }
        
        return review;
    }

    // Private helper method to update professional's rating
    private void updateProfessionalRating(ProfessionalProfile professional) {
        Double averageRating = reviewRepository.findAverageRatingByProfessional(professional);
        Long totalReviews = reviewRepository.countReviewsByProfessional(professional);
        
        professional.setAverageRating(averageRating != null ? averageRating : 0.0);
        professional.setTotalReviews(totalReviews.intValue());
        
        profileRepository.save(professional);
    }
}