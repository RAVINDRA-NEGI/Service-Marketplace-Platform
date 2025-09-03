package com.marketplace.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.ReviewDto;
import com.marketplace.enums.BookingStatus;
import com.marketplace.exception.AccessDeniedException;
import com.marketplace.exception.BookingNotFoundException;
import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.exception.ReviewAlreadyExistsException;
import com.marketplace.exception.ReviewNotFoundException;
import com.marketplace.exception.ValidationException;
import com.marketplace.model.Booking;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;
import com.marketplace.repository.BookingRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.ReviewRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ReviewService;

@Service("reviewService")
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final int REVIEW_WINDOW_DAYS = 30; // Reviews must be submitted within 30 days

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
        logger.info("Creating review for client ID: {} and professional ID: {}", 
                   client.getId(), reviewDto.getProfessionalId());

        // Validate input
        validateReviewDto(reviewDto);

        // Get booking with validation
        Booking booking = getValidatedBooking(reviewDto.getBookingId(), client);

        // Verify professional ID matches booking
        if (!booking.getProfessional().getId().equals(reviewDto.getProfessionalId())) {
            throw new ValidationException("Professional ID does not match the booking");
        }

        // Verify booking is within review window
        validateReviewWindow(booking);

        // Check if already reviewed
        if (reviewRepository.existsByBookingAndDeletedFalse(booking)) {
            throw new ReviewAlreadyExistsException("You have already reviewed this booking");
        }

        // Get professional
        ProfessionalProfile professional = profileRepository.findById(reviewDto.getProfessionalId())
                .orElseThrow(() -> new ProfessionalNotFoundException("Professional not found"));

        // Create review
        Review review = new Review(professional, client, booking, reviewDto.getRating(), reviewDto.getComment());
        Review savedReview = reviewRepository.save(review);

        // Update professional's average rating asynchronously or in same transaction
        updateProfessionalRating(professional);

        logger.info("Review created successfully with ID: {}", savedReview.getId());
        return savedReview;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getProfessionalReviews(ProfessionalProfile professional) {
        return reviewRepository.findByProfessionalAndDeletedFalseOrderByCreatedAtDesc(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getClientReviews(User client) {
        return reviewRepository.findByClientAndDeletedFalseOrderByCreatedAtDesc(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewById(Long id) {
        return reviewRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewByBooking(Booking booking) {
        return reviewRepository.findByBookingAndDeletedFalse(booking)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for this booking"));
    }

    @Override
    @Transactional
    public Review updateReview(Long reviewId, User client, ReviewDto reviewDto) {
        logger.info("Updating review ID: {} for client ID: {}", reviewId, client.getId());
        
        Review review = getReviewByIdAndClient(reviewId, client);
        
        // Validate input
        validateReviewDto(reviewDto);
        
        // Update fields
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        
        Review updatedReview = reviewRepository.save(review);
        
        // Update professional's average rating
        updateProfessionalRating(review.getProfessional());
        
        logger.info("Review updated successfully with ID: {}", reviewId);
        return updatedReview;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, User client) {
        logger.info("Deleting review ID: {} for client ID: {}", reviewId, client.getId());
        
        Review review = getReviewByIdAndClient(reviewId, client);
        ProfessionalProfile professional = review.getProfessional();
        
        // Soft delete
        review.markAsDeleted();
        reviewRepository.save(review);
        
        // Update professional's average rating
        updateProfessionalRating(professional);
        
        logger.info("Review soft deleted successfully with ID: {}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReviewBooking(User client, Long bookingId) {
        try {
            Booking booking = getValidatedBooking(bookingId, client);
            validateReviewWindow(booking);
            return !reviewRepository.existsByBookingAndDeletedFalse(booking);
        } catch (Exception e) {
            logger.debug("Cannot review booking ID: {} for client ID: {} - {}", 
                        bookingId, client.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReviewedProfessional(User client, ProfessionalProfile professional) {
        return reviewRepository.existsByProfessionalAndClientAndDeletedFalse(professional, client);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(ProfessionalProfile professional) {
        return reviewRepository.findAverageRatingByProfessionalAndDeletedFalse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalReviews(ProfessionalProfile professional) {
        return reviewRepository.countReviewsByProfessionalAndDeletedFalse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getRecentReviews(ProfessionalProfile professional) {
        return reviewRepository.findRecentReviewsByProfessionalAndDeletedFalse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReviewByIdAndClient(Long id, User client) {
        Review review = getReviewById(id);
        
        if (!review.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("You can only access your own reviews");
        }
        
        return review;
    }

    // Private helper methods
    
    private void validateReviewDto(ReviewDto reviewDto) {
        if (reviewDto.getRating() < Review.MIN_RATING || reviewDto.getRating() > Review.MAX_RATING) {
            throw new ValidationException("Rating must be between " + Review.MIN_RATING + " and " + Review.MAX_RATING);
        }
        
        if (reviewDto.getComment() == null || reviewDto.getComment().trim().length() < Review.MIN_COMMENT_LENGTH) {
            throw new ValidationException("Comment must be at least " + Review.MIN_COMMENT_LENGTH + " characters long");
        }
        
        if (reviewDto.getComment().length() > Review.MAX_COMMENT_LENGTH) {
            throw new ValidationException("Comment cannot exceed " + Review.MAX_COMMENT_LENGTH + " characters");
        }
    }
    
    private Booking getValidatedBooking(Long bookingId, User client) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        // Verify booking belongs to client
        if (!booking.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("You can only review your own bookings");
        }

        // Verify booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ValidationException("You can only review completed bookings");
        }
        
        return booking;
    }
    
    private void validateReviewWindow(Booking booking) {
        LocalDateTime reviewDeadline = booking.getUpdatedAt().plusDays(REVIEW_WINDOW_DAYS);
        if (LocalDateTime.now().isAfter(reviewDeadline)) {
            throw new ValidationException("Review window has expired. Reviews must be submitted within " 
                                        + REVIEW_WINDOW_DAYS + " days of booking completion");
        }
    }

    private void updateProfessionalRating(ProfessionalProfile professional) {
        try {
            Double averageRating = reviewRepository.findAverageRatingByProfessionalAndDeletedFalse(professional);
            Long totalReviews = reviewRepository.countReviewsByProfessionalAndDeletedFalse(professional);
            
            professional.setAverageRating(averageRating != null ? averageRating : 0.0);
            professional.setTotalReviews(totalReviews.intValue());
            
            profileRepository.save(professional);
            
            logger.debug("Updated professional ID: {} rating to {} ({} reviews)", 
                        professional.getId(), averageRating, totalReviews);
        } catch (Exception e) {
            logger.error("Failed to update professional rating for ID: " + professional.getId(), e);
            // Don't throw here as this is a secondary operation
        }
    }
}