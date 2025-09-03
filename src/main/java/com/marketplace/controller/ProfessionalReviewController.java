package com.marketplace.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.Review;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.ReviewService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/professional/reviews")
@PreAuthorize("hasRole('PROFESSIONAL')")
public class ProfessionalReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfessionalReviewController.class);
    
    private final ReviewService reviewService;
    private final ProfessionalService professionalService;
    private final UserService userService;
    
    public ProfessionalReviewController(ReviewService reviewService, 
                                      ProfessionalService professionalService, 
                                      UserService userService) {
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
        
        try {
            ProfessionalProfile professional = professionalService.getProfileByUser(currentUser);
            List<Review> reviews = reviewService.getProfessionalReviews(professional);
            Double averageRating = reviewService.getAverageRating(professional);
            Long totalReviews = reviewService.getTotalReviews(professional);
            
            // Simple attributes - no complex calculations
            model.addAttribute("reviews", reviews != null ? reviews : List.of());
            model.addAttribute("averageRating", averageRating != null ? averageRating : 0.0);
            model.addAttribute("totalReviews", totalReviews != null ? totalReviews : 0L);
            model.addAttribute("professional", professional);
            
            logger.info("Loaded {} reviews for professional ID: {}", 
                       reviews != null ? reviews.size() : 0, professional.getId());
            return "professional/my-reviews";
            
        } catch (ProfessionalNotFoundException e) {
            logger.warn("Professional profile not found for user ID: {}", currentUser.getId());
            model.addAttribute("error", "Professional profile not found");
            return "error";
        } catch (Exception e) {
            logger.error("Error loading reviews for professional user ID: " + currentUser.getId(), e);
            model.addAttribute("error", "An error occurred while loading your reviews");
            
            // Safe defaults
            model.addAttribute("reviews", List.of());
            model.addAttribute("averageRating", 0.0);
            model.addAttribute("totalReviews", 0L);
            return "professional/my-reviews";
        }
    }
    
    @GetMapping("/recent")
    public String listRecentReviews(Model model) {
        User currentUser = getCurrentUser();
        
        try {
            ProfessionalProfile professional = professionalService.getProfileByUser(currentUser);
            List<Review> recentReviews = reviewService.getRecentReviews(professional);
            
            model.addAttribute("reviews", recentReviews != null ? recentReviews : List.of());
            model.addAttribute("professional", professional);
            
            logger.info("Loaded {} recent reviews for professional ID: {}", 
                       recentReviews != null ? recentReviews.size() : 0, professional.getId());
            return "professional/recent-reviews";
            
        } catch (ProfessionalNotFoundException e) {
            logger.warn("Professional profile not found for user ID: {}", currentUser.getId());
            model.addAttribute("error", "Professional profile not found");
            return "error";
        } catch (Exception e) {
            logger.error("Error loading recent reviews for professional user ID: " + currentUser.getId(), e);
            model.addAttribute("error", "An error occurred while loading your recent reviews");
            model.addAttribute("reviews", List.of());
            return "professional/recent-reviews";
        }
    }
}
