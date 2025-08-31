package com.marketplace.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.BookingDto;
import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.exception.UnauthorizedAccessException;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.AvailabilityService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/client")
public class BrowseController {

    private static final Logger logger = LoggerFactory.getLogger(BrowseController.class);

    private final ProfessionalService professionalService;
    private final UserService userService;
    private final AvailabilityService availabilityService;
    
    public BrowseController(ProfessionalService professionalService, UserService userService, AvailabilityService availabilityService) {
        this.professionalService = professionalService;
        this.userService = userService;
        this.availabilityService = availabilityService;
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

    // Helper method to check authentication
    private User requireAuthentication() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required");
        }
        return currentUser;
    }

    @GetMapping("/professionals")
    public String browseProfessionals(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ProfessionalProfile> professionals = professionalService.searchProfessionals(category, city, minRating, pageable);
            
            model.addAttribute("professionals", professionals);
            model.addAttribute("categories", professionalService.getAllCategories());
            model.addAttribute("currentCategory", category);
            model.addAttribute("currentCity", city);
            model.addAttribute("currentMinRating", minRating);
            
            return "client/browse-professionals";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Error browsing professionals", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while searching for professionals");
            return "redirect:/client/dashboard";
        }
    }

    @GetMapping("/professional/{id}")
    public String viewProfessionalProfile(
            @PathVariable Long id, 
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "weekOffset", defaultValue = "0") int weekOffset,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            // Validate professional ID
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid professional ID");
                return "redirect:/client/professionals";
            }

            ProfessionalProfile profile = professionalService.getProfileById(id);
            
            // Handle date selection with validation
            LocalDate selectedDate;
            try {
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    selectedDate = LocalDate.parse(dateStr);
                } else {
                    selectedDate = LocalDate.now().plusWeeks(weekOffset);
                }
                
                // Validate date is not in the past
                if (selectedDate.isBefore(LocalDate.now())) {
                    selectedDate = LocalDate.now();
                    model.addAttribute("warning", "Selected date was in the past. Showing availability from today.");
                }
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format provided: {}", dateStr);
                selectedDate = LocalDate.now().plusWeeks(weekOffset);
                model.addAttribute("warning", "Invalid date format. Showing availability from current date.");
            }
            
            // Get available slots for the next 30 days from selected date
            LocalDate endDate = selectedDate.plusDays(30);
            List<Availability> availableSlots = 
                availabilityService.getAvailableSlots(profile.getUser(), selectedDate, endDate);
            
            model.addAttribute("profile", profile);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("availableSlots", availableSlots);
            model.addAttribute("weekOffset", weekOffset);
            model.addAttribute("bookingDto", new BookingDto());
            
            return "client/professional-detail";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (ProfessionalNotFoundException e) {
            logger.warn("Professional not found with ID: {}", id);
            redirectAttributes.addFlashAttribute("error", "Professional not found");
            return "redirect:/client/professionals";
        } catch (Exception e) {
            logger.error("Error viewing professional profile for ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while loading the professional profile");
            return "redirect:/client/professionals";
        }
    }
}