package com.marketplace.controller;

import java.time.LocalDate;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userService.findById(userDetails.getId()).orElse(null);
        }
        return null;
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
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
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
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("error", "Please correct the errors below");
            return showAvailabilityManagement(null, model);
        }

        try {
            Availability availability = availabilityService.createAvailability(currentUser, availabilityDto);
            redirectAttributes.addFlashAttribute("message", "Availability slot added successfully!");
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professional/availability?date=" + availabilityDto.getDate();
        }
    }

    @PostMapping("/bulk-add")
    public String addBulkAvailability(@Valid @ModelAttribute("availabilityDto") AvailabilityDto availabilityDto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            List<Availability> availabilities = availabilityService.createBulkAvailability(currentUser, availabilityDto);
            redirectAttributes.addFlashAttribute("message", 
                "Bulk availability added successfully! " + availabilities.size() + " slots created.");
            return "redirect:/professional/availability";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/professional/availability";
    }
}