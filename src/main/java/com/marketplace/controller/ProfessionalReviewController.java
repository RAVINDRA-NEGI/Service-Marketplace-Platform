package com.marketplace.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.ReviewService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/professional/reviews")
public class ProfessionalReviewController {

    private final ReviewService reviewService;
    private final ProfessionalService professionalService;
    private final UserService userService;

    public ProfessionalReviewController(ReviewService reviewService, ProfessionalService professionalService, UserService userService) {
        this.reviewService = reviewService;
        this.professionalService = professionalService;
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

    @GetMapping
    public String listProfessionalReviews(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ProfessionalProfile professional = professionalService.getProfileByUser(currentUser);
            List<Review> reviews = reviewService.getProfessionalReviews(professional);
            Double averageRating = reviewService.getAverageRating(professional);
            Long totalReviews = reviewService.getTotalReviews(professional);
            
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("professional", professional);
            
            return "professional/my-reviews";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "professional/my-reviews";
        }
    }

    @GetMapping("/recent")
    public String listRecentReviews(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ProfessionalProfile professional = professionalService.getProfileByUser(currentUser);
            List<Review> recentReviews = reviewService.getRecentReviews(professional);
            
            model.addAttribute("reviews", recentReviews);
            model.addAttribute("professional", professional);
            
            return "professional/recent-reviews";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "professional/recent-reviews";
        }
    }
}