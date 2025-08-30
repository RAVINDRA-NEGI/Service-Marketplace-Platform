package com.marketplace.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.BookingDto;
import com.marketplace.exception.AvailabilityNotFoundException;
import com.marketplace.exception.BookingException;
import com.marketplace.exception.ProfessionalNotFoundException;
import com.marketplace.exception.SlotNotAvailableException;
import com.marketplace.exception.UnauthorizedAccessException;
import com.marketplace.model.Availability;
import com.marketplace.model.Booking;
import com.marketplace.model.Booking.BookingStatus;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.AvailabilityService;
import com.marketplace.service.BookingService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/client/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final ProfessionalService professionalService;
    private final UserService userService;
    private final AvailabilityService availabilityService;
    

    public BookingController(BookingService bookingService, ProfessionalService professionalService, 
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

    @GetMapping("/professional/{professionalId}")
    public String viewProfessionalAvailability(
            @PathVariable Long professionalId,
            @RequestParam(value = "date", required = false) String dateStr,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();
            
            // Validate professional ID
            if (professionalId == null || professionalId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid professional ID");
                return "redirect:/client/search";
            }

            ProfessionalProfile professional = professionalService.getProfileById(professionalId);
            
            LocalDate selectedDate;
            try {
                selectedDate = dateStr != null && !dateStr.trim().isEmpty() 
                    ? LocalDate.parse(dateStr) 
                    : LocalDate.now().plusDays(1);
                    
                // Validate date is not in the past
                if (selectedDate.isBefore(LocalDate.now())) {
                    selectedDate = LocalDate.now().plusDays(1);
                    model.addAttribute("warning", "Selected date was in the past. Showing availability from tomorrow.");
                }
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format provided: {}", dateStr);
                selectedDate = LocalDate.now().plusDays(1);
                model.addAttribute("warning", "Invalid date format. Showing availability from tomorrow.");
            }
            
            // Get available slots for the next 30 days
            LocalDate endDate = selectedDate.plusDays(30);
            List<Availability> availableSlots = 
               availabilityService.getAvailableSlots(professional.getUser(), selectedDate, endDate);
            
            model.addAttribute("professional", professional);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("availableSlots", availableSlots);
            model.addAttribute("bookingDto", new BookingDto());
            
            return "client/book-availability";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (ProfessionalNotFoundException e) {
            logger.warn("Professional not found with ID: {}", professionalId);
            redirectAttributes.addFlashAttribute("error", "Professional not found");
            return "redirect:/client/search";
        } catch (Exception e) {
            logger.error("Error viewing professional availability for ID: " + professionalId, e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while loading availability");
            return "redirect:/client/search";
        }
    }

    @PostMapping("/create")
    public String createBooking(
            @Valid @ModelAttribute("bookingDto") BookingDto bookingDto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = requireAuthentication();

            if (result.hasErrors()) {
                StringBuilder errorMessages = new StringBuilder("Please correct the following errors: ");
                result.getAllErrors().forEach(error -> 
                    errorMessages.append(error.getDefaultMessage()).append("; "));
                redirectAttributes.addFlashAttribute("error", errorMessages.toString());
                return "redirect:/client/bookings/professional/" + bookingDto.getProfessionalId();
            }

            Booking booking = bookingService.createBooking(currentUser, bookingDto);
            redirectAttributes.addFlashAttribute("message", 
                "Booking created successfully! Awaiting confirmation. Booking ID: " + booking.getId());
            return "redirect:/client/bookings";
            
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login";
        } catch (SlotNotAvailableException e) {
            logger.warn("Slot not available for booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/bookings/professional/" + bookingDto.getProfessionalId();
        } catch (ProfessionalNotFoundException | AvailabilityNotFoundException e) {
            logger.warn("Resource not found during booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/search";
        } catch (BookingException e) {
            logger.error("Business logic error during booking creation", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/bookings/professional/" + 
                (bookingDto.getProfessionalId() != null ? bookingDto.getProfessionalId() : "");
        } catch (Exception e) {
            logger.error("Unexpected error during booking creation", e);
            redirectAttributes.addFlashAttribute("error", 
                "An unexpected error occurred while creating your booking. Please try again.");
            return "redirect:/client/bookings/professional/" + 
                (bookingDto.getProfessionalId() != null ? bookingDto.getProfessionalId() : "");
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