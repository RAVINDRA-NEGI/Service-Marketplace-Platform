package com.marketplace.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

import com.marketplace.dto.AvailabilityDto;
import com.marketplace.model.Availability;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.AvailabilityService;
import com.marketplace.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/professional/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final UserService userService;

    public AvailabilityController(AvailabilityService availabilityService, UserService userService) {
        this.availabilityService = availabilityService;
        this.userService = userService;
    }

    // Helper method to get current user
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || 
                !(auth.getPrincipal() instanceof UserDetailsImpl)) {
                return null;
            }
            
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            if (userDetails.getId() == null) {
                return null;
            }
            
            return userService.findById(userDetails.getId()).orElse(null);
        } catch (Exception e) {
            // Log the error if you have logging
            return null;
        }
    }

    @GetMapping
    public String showAvailabilityManagement(
            @RequestParam(value = "date", required = false) String dateStr,
            Model model) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            LocalDate selectedDate = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            List<Availability> availabilities = availabilityService.getAvailabilityByDate(currentUser, selectedDate);
            List<Availability> allAvailabilities = availabilityService.getProfessionalAvailability(currentUser);
            
            // Filter future availabilities in the controller
            LocalDate today = LocalDate.now();
            List<Availability> futureAvailabilities = allAvailabilities.stream()
                    .filter(slot -> slot.getDate().isAfter(today) || slot.getDate().isEqual(today))
                    .collect(Collectors.toList());
            
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("availabilities", availabilities);
            model.addAttribute("allAvailabilities", allAvailabilities);
            model.addAttribute("futureAvailabilities", futureAvailabilities);
            model.addAttribute("hasFutureAvailabilities", !futureAvailabilities.isEmpty());
            model.addAttribute("availabilityDto", new AvailabilityDto());
            
            return "professional/availability-management";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid date format. Please select a valid date.");
            model.addAttribute("selectedDate", LocalDate.now());
            model.addAttribute("availabilities", new ArrayList<>());
            model.addAttribute("allAvailabilities", new ArrayList<>());
            model.addAttribute("futureAvailabilities", new ArrayList<>());
            model.addAttribute("hasFutureAvailabilities", false);
            model.addAttribute("availabilityDto", new AvailabilityDto());
            return "professional/availability-management";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while loading availability data.");
            model.addAttribute("selectedDate", LocalDate.now());
            model.addAttribute("availabilities", new ArrayList<>());
            model.addAttribute("allAvailabilities", new ArrayList<>());
            model.addAttribute("futureAvailabilities", new ArrayList<>());
            model.addAttribute("hasFutureAvailabilities", false);
            model.addAttribute("availabilityDto", new AvailabilityDto());
            return "professional/availability-management";
        }
    }

    @PostMapping("/add")
    public String addAvailability(@Valid @ModelAttribute("availabilityDto") AvailabilityDto availabilityDto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to continue");
            return "redirect:/login";
        }

        // Basic time validation
        if (availabilityDto.getStartTime() != null && availabilityDto.getEndTime() != null &&
            !availabilityDto.getStartTime().isBefore(availabilityDto.getEndTime())) {
            result.rejectValue("endTime", "error.endTime", "End time must be after start time");
        }

        if (result.hasErrors()) {
            // Preserve form data and show errors on the same page
            model.addAttribute("error", "Please correct the errors below");
            model.addAttribute("availabilityDto", availabilityDto);
            model.addAttribute("validationErrors", result);
            return showAvailabilityManagement(availabilityDto.getDate() != null ? 
                                            availabilityDto.getDate().toString() : null, model);
        }

        try {
            availabilityService.createAvailability(currentUser, availabilityDto);
            redirectAttributes.addFlashAttribute("message", "Availability slot added successfully!");
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid input: " + e.getMessage());
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while adding availability");
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        }
    }

    @PostMapping("/bulk-add")
    public String addBulkAvailability(@Valid @ModelAttribute("availabilityDto") AvailabilityDto availabilityDto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to continue");
            return "redirect:/login";
        }

        // Basic validation for bulk fields
        if (availabilityDto.getBulkStartTime() != null && availabilityDto.getBulkEndTime() != null &&
            !availabilityDto.getBulkStartTime().isBefore(availabilityDto.getBulkEndTime())) {
            redirectAttributes.addFlashAttribute("error", "End time must be after start time");
            return "redirect:/professional/availability";
        }

        if (availabilityDto.getDates() == null || availabilityDto.getDates().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select dates for bulk availability");
            return "redirect:/professional/availability";
        }

        try {
            List<Availability> availabilities = availabilityService.createBulkAvailability(currentUser, availabilityDto);
            redirectAttributes.addFlashAttribute("message", 
                "Bulk availability added successfully! " + availabilities.size() + " slots created.");
            return "redirect:/professional/availability";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid input: " + e.getMessage());
            return "redirect:/professional/availability";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professional/availability";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while adding bulk availability");
            return "redirect:/professional/availability";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAvailability(@PathVariable Long id,
                                   @RequestParam(required = false) String date,
                                   RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            availabilityService.deleteAvailability(currentUser, id);
            redirectAttributes.addFlashAttribute("message", "Availability slot deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        String redirectUrl = "/professional/availability";
        if (date != null) {
            redirectUrl += "?date=" + date;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/delete-by-date")
    public String deleteAvailabilityByDate(@RequestParam String date,
                                         RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            LocalDate localDate = LocalDate.parse(date);
            availabilityService.deleteAvailabilityByDate(currentUser, localDate);
            redirectAttributes.addFlashAttribute("message", "All availability slots for " + date + " deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date format");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while deleting availability slots");
        }

        return "redirect:/professional/availability";
    }
}