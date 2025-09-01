package com.marketplace.controller;

import java.util.List;

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

import com.marketplace.enums.BookingStatus;
import com.marketplace.model.Booking;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.BookingService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/professional/bookings")
public class ProfessionalBookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public ProfessionalBookingController(BookingService bookingService, UserService userService) {
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

    @GetMapping
    public String listProfessionalBookings(
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            List<Booking> bookings;
            if (status != null && !status.isEmpty()) {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingService.getProfessionalBookingsByStatus(currentUser, bookingStatus);
                model.addAttribute("currentStatus", status);
            } else {
                bookings = bookingService.getProfessionalBookingsWithDetails(currentUser);
            }
            
            model.addAttribute("bookings", bookings);
            return "professional/bookings";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "professional/bookings";
        }
    }

    @GetMapping("/{id}")
    public String viewBookingDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            if (!bookingService.canManageBooking(id, currentUser)) {
                model.addAttribute("error", "Access denied");
                return "error";
            }
            
            Booking booking = bookingService.getBookingById(id);
            model.addAttribute("booking", booking);
            return "professional/booking-detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "professional/bookings";
        }
    }

    @PostMapping("/accept/{id}")
    public String acceptBooking(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.acceptBooking(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Booking accepted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/professional/bookings";
    }

    @PostMapping("/reject/{id}")
    public String rejectBooking(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.rejectBooking(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Booking rejected successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/professional/bookings";
    }

    @PostMapping("/complete/{id}")
    public String markAsCompleted(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.markAsCompleted(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Booking marked as completed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/professional/bookings";
    }
}