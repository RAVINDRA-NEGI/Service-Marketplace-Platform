package com.marketplace.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.BookingDto;
import com.marketplace.enums.BookingStatus;
import com.marketplace.exception.AvailabilityNotFoundException;
import com.marketplace.exception.BookingException;
import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.exception.SlotNotAvailableException;
import com.marketplace.exception.UnauthorizedAccessException;
import com.marketplace.model.Booking;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.AvailabilityService;
import com.marketplace.service.BookingService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/client/bookings")
public class UserBookingController {

    private static final Logger logger = LoggerFactory.getLogger(UserBookingController.class);

    private final BookingService bookingService;
    private final ProfessionalService professionalService;
    private final UserService userService;
    private final AvailabilityService availabilityService;
    

    public UserBookingController(BookingService bookingService, ProfessionalService professionalService, 
                           UserService userService, AvailabilityService availabilityService) {
        this.bookingService = bookingService;
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

    @GetMapping
    public String listClientBookings(
            @RequestParam(value = "status", required = false) String status,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            List<Booking> bookings;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                    bookings = bookingService.getClientBookingsByStatus(currentUser, bookingStatus);
                    model.addAttribute("currentStatus", status);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid booking status requested: {}", status);
                    bookings = bookingService.getClientBookings(currentUser);
                    model.addAttribute("error", "Invalid status filter. Showing all bookings.");
                }
            } else {
                bookings = bookingService.getClientBookings(currentUser);
            }
            
            model.addAttribute("bookings", bookings);
            return "client/bookings";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (BookingException e) {
            logger.error("Error retrieving client bookings", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("bookings", List.of()); // Empty list to avoid null pointer
            return "client/bookings";
        } catch (Exception e) {
            logger.error("Unexpected error retrieving client bookings", e);
            model.addAttribute("error", "An unexpected error occurred while retrieving your bookings");
            model.addAttribute("bookings", List.of());
            return "client/bookings";
        }
    }

    @PostMapping("/create")
    public String createBooking(
            @RequestParam("professionalId") Long professionalId,
            @RequestParam("availabilityId") Long availabilityId,
            @RequestParam(value = "serviceDetails", required = false) String serviceDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();

            // Validate required parameters
            if (professionalId == null || professionalId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid professional ID");
                return "redirect:/client/professionals";
            }
            
            if (availabilityId == null || availabilityId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid availability slot");
                return "redirect:/client/professional/" + professionalId;
            }

            // Create BookingDto from form parameters
            BookingDto bookingDto = new BookingDto();
            bookingDto.setProfessionalId(professionalId);
            bookingDto.setAvailabilityId(availabilityId);
            bookingDto.setServiceDetails(serviceDetails);

            Booking booking = bookingService.createBooking(currentUser, bookingDto);
            redirectAttributes.addFlashAttribute("message", 
                "Booking created successfully! Awaiting confirmation. Booking ID: " + booking.getId());
            return "redirect:/client/bookings";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (SlotNotAvailableException e) {
            logger.warn("Slot not available for booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/professional/" + professionalId;
        } catch (ProfessionalNotFoundException | AvailabilityNotFoundException e) {
            logger.warn("Resource not found during booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/professionals";
        } catch (BookingException e) {
            logger.error("Business logic error during booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/professional/" + (professionalId != null ? professionalId : "");
        } catch (Exception e) {
            logger.error("Unexpected error during booking creation", e);
            redirectAttributes.addFlashAttribute("error", 
                "An unexpected error occurred while creating your booking. Please try again.");
            return "redirect:/client/professional/" + (professionalId != null ? professionalId : "");
        }
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            // Validate booking ID
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking ID");
                return "redirect:/client/bookings";
            }

            bookingService.cancelBooking(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Booking cancelled successfully!");
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (BookingException e) {
            logger.error("Error cancelling booking with ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error cancelling booking with ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", 
                "An unexpected error occurred while cancelling your booking. Please try again.");
        }

        return "redirect:/client/bookings";
    }

    // Additional helper endpoint to check if user can book a specific slot
    @GetMapping("/check-availability/{availabilityId}")
    public String checkSlotAvailability(
            @PathVariable Long availabilityId,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            if (availabilityId == null || availabilityId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid availability ID");
                return "redirect:/client/bookings";
            }
            
            boolean canBook = bookingService.canBookSlot(availabilityId, currentUser);
            if (canBook) {
                redirectAttributes.addFlashAttribute("message", "Slot is available for booking!");
            } else {
                redirectAttributes.addFlashAttribute("error", "This slot is no longer available");
            }
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error checking slot availability", e);
            redirectAttributes.addFlashAttribute("error", "Unable to check slot availability");
        }
        
        return "redirect:/client/bookings";
    }
}