package com.marketplace.controller;

import java.util.List;

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
public class ReviewController {

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
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Check if user can review this booking
            if (!reviewService.canReviewBooking(currentUser, bookingId)) {
                model.addAttribute("error", "You cannot review this booking");
                return "error";
            }

            Booking booking = bookingService.getBookingById(bookingId);
            
            ReviewDto reviewDto = new ReviewDto();
            reviewDto.setProfessionalId(booking.getProfessional().getId());
            reviewDto.setBookingId(bookingId);
            
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("booking", booking);
            
            return "client/create-review";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/create")
    public String createReview(@Valid @ModelAttribute("reviewDto") ReviewDto reviewDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors below");
            return "redirect:/client/reviews/create/" + reviewDto.getBookingId();
        }

        try {
            Review review = reviewService.createReview(currentUser, reviewDto);
            redirectAttributes.addFlashAttribute("message", "Review submitted successfully! Thank you for your feedback.");
            return "redirect:/client/bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reviews/create/" + reviewDto.getBookingId();
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditReviewForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Review review = reviewService.getReviewByIdAndClient(id, currentUser);
            
            ReviewDto reviewDto = new ReviewDto();
            reviewDto.setId(review.getId());
            reviewDto.setProfessionalId(review.getProfessional().getId());
            reviewDto.setBookingId(review.getBooking().getId());
            reviewDto.setRating(review.getRating());
            reviewDto.setComment(review.getComment());
            
            model.addAttribute("reviewDto", reviewDto);
            model.addAttribute("review", review);
            
            return "client/edit-review";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/edit")
    public String updateReview(@Valid @ModelAttribute("reviewDto") ReviewDto reviewDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors below");
            return "redirect:/client/reviews/edit/" + reviewDto.getId();
        }

        try {
            Review review = reviewService.updateReview(reviewDto.getId(), currentUser, reviewDto);
            redirectAttributes.addFlashAttribute("message", "Review updated successfully!");
            return "redirect:/client/bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/reviews/edit/" + reviewDto.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            reviewService.deleteReview(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Review deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/bookings";
    }

    @GetMapping("/my-reviews")
    public String listClientReviews(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            List<Review> reviews = reviewService.getClientReviews(currentUser);
            model.addAttribute("reviews", reviews);
            return "client/my-reviews";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "client/my-reviews";
        }
    }
}