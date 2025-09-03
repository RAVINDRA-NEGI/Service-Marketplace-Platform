package com.marketplace.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.ReviewDto;
import com.marketplace.exception.AccessDeniedException;
import com.marketplace.exception.BookingNotFoundException;
import com.marketplace.exception.ReviewAlreadyExistsException;
import com.marketplace.exception.ReviewNotFoundException;
import com.marketplace.exception.ValidationException;
import com.marketplace.model.Booking;
import com.marketplace.model.Review;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.BookingService;
import com.marketplace.service.ReviewService;
import com.marketplace.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/client/reviews")
@PreAuthorize("hasRole('CLIENT')")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, BookingService bookingService, UserService userService) {
        this.reviewService = reviewService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    // Helper method to get current user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userService.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    @GetMapping("/create/{bookingId}")
    public String showCreateReviewForm(@PathVariable Long bookingId, Model model) {
        User currentUser = getCurrentUser();
        
        try {
            // Check if user can review this booking
            if (!reviewService.canReviewBooking(currentUser, bookingId)) {
                model.addAttribute("error", "You cannot review this booking. It may not be completed, already reviewed, or the review window has expired.");
                return "error";
            }

            Booking booking = bookingService.getBookingById(bookingId);
            
            ReviewDto reviewDto = new ReviewDto();
            reviewDto.setProfessionalId(booking.getProfessional().getId());
            reviewDto.setBookingId(bookingId);
            
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("booking", booking);
            
            return "client/create-review";
            
        } catch (BookingNotFoundException e) {
            logger.warn("Booking not found for ID: {} by user: {}", bookingId, currentUser.getId());
            model.addAttribute("error", "Booking not found");
            return "error";
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for booking ID: {} by user: {}", bookingId, currentUser.getId());
            model.addAttribute("error", "Access denied");
            return "error";
        } catch (Exception e) {
            logger.error("Error showing create review form for booking: " + bookingId, e);
            model.addAttribute("error", "An unexpected error occurred");
            return "error";
        }
    }

    @PostMapping("/create")
    public String createReview(@Valid @ModelAttribute("reviewDto") ReviewDto reviewDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors and try again");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewDto", result);
            redirectAttributes.addFlashAttribute("reviewDto", reviewDto);
            return "redirect:/client/reviews/create/" + reviewDto.getBookingId();
        }

        try {
            Review review = reviewService.createReview(currentUser, reviewDto);
            redirectAttributes.addFlashAttribute("message", "Review submitted successfully! Thank you for your feedback.");
            logger.info("Review created successfully with ID: {} for user: {}", review.getId(), currentUser.getId());
            return "redirect:/client/bookings";
            
        } catch (ReviewAlreadyExistsException e) {
            logger.warn("Attempted to create duplicate review for booking: {} by user: {}", 
                       reviewDto.getBookingId(), currentUser.getId());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/bookings";
        } catch (ValidationException e) {
            logger.warn("Validation error creating review: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reviews/create/" + reviewDto.getBookingId();
        } catch (AccessDeniedException e) {
            logger.warn("Access denied creating review for booking: {} by user: {}", 
                       reviewDto.getBookingId(), currentUser.getId());
            redirectAttributes.addFlashAttribute("error", "Access denied");
            return "redirect:/client/bookings";
        } catch (Exception e) {
            logger.error("Error creating review for booking: " + reviewDto.getBookingId(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while submitting your review");
            return "redirect:/client/reviews/create/" + reviewDto.getBookingId();
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditReviewForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();

        try {
            Review review = reviewService.getReviewByIdAndClient(id, currentUser);
            
            if (!review.canBeEditedBy(currentUser)) {
                model.addAttribute("error", "This review cannot be edited");
                return "error";
            }
            
            ReviewDto reviewDto = new ReviewDto();
            reviewDto.setId(review.getId());
            reviewDto.setProfessionalId(review.getProfessional().getId());
            reviewDto.setBookingId(review.getBooking().getId());
            reviewDto.setRating(review.getRating());
            reviewDto.setComment(review.getComment());
            
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("review", review);
            
            return "client/edit-review";
            
        } catch (ReviewNotFoundException e) {
            logger.warn("Review not found for ID: {} by user: {}", id, currentUser.getId());
            model.addAttribute("error", "Review not found");
            return "error";
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for review ID: {} by user: {}", id, currentUser.getId());
            model.addAttribute("error", "Access denied");
            return "error";
        } catch (Exception e) {
            logger.error("Error showing edit review form for ID: " + id, e);
            model.addAttribute("error", "An unexpected error occurred");
            return "error";
        }
    }

    @PostMapping("/edit")
    public String updateReview(@Valid @ModelAttribute("reviewDto") ReviewDto reviewDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors and try again");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewDto", result);
            redirectAttributes.addFlashAttribute("reviewDto", reviewDto);
            return "redirect:/client/reviews/edit/" + reviewDto.getId();
        }

        try {
            Review review = reviewService.updateReview(reviewDto.getId(), currentUser, reviewDto);
            redirectAttributes.addFlashAttribute("message", "Review updated successfully!");
            logger.info("Review updated successfully with ID: {} for user: {}", review.getId(), currentUser.getId());
            return "redirect:/client/bookings";
            
        } catch (ReviewNotFoundException e) {
            logger.warn("Review not found for ID: {} by user: {}", reviewDto.getId(), currentUser.getId());
            redirectAttributes.addFlashAttribute("error", "Review not found");
            return "redirect:/client/bookings";
        } catch (ValidationException e) {
            logger.warn("Validation error updating review: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reviews/edit/" + reviewDto.getId();
        } catch (AccessDeniedException e) {
            logger.warn("Access denied updating review ID: {} by user: {}", reviewDto.getId(), currentUser.getId());
            redirectAttributes.addFlashAttribute("error", "Access denied");
            return "redirect:/client/bookings";
        } catch (Exception e) {
            logger.error("Error updating review ID: " + reviewDto.getId(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while updating your review");
            return "redirect:/client/reviews/edit/" + reviewDto.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        try {
            reviewService.deleteReview(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Review deleted successfully!");
            logger.info("Review deleted successfully with ID: {} for user: {}", id, currentUser.getId());
            
        } catch (ReviewNotFoundException e) {
            logger.warn("Review not found for deletion ID: {} by user: {}", id, currentUser.getId());
            redirectAttributes.addFlashAttribute("error", "Review not found");
        } catch (AccessDeniedException e) {
            logger.warn("Access denied deleting review ID: {} by user: {}", id, currentUser.getId());
            redirectAttributes.addFlashAttribute("error", "Access denied");
        } catch (Exception e) {
            logger.error("Error deleting review ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while deleting your review");
        }

        return "redirect:/client/bookings";
    }

    @GetMapping("/my-reviews")
    public String listClientReviews(Model model) {
        User currentUser = getCurrentUser();

        try {
            List<Review> reviews = reviewService.getClientReviews(currentUser);
            model.addAttribute("reviews", reviews);
            return "client/my-reviews";
            
        } catch (Exception e) {
            logger.error("Error listing client reviews for user: " + currentUser.getId(), e);
            model.addAttribute("error", "An error occurred while loading your reviews");
            model.addAttribute("reviews", List.of()); // Empty list to prevent template errors
            return "client/my-reviews";
        }
    }
}