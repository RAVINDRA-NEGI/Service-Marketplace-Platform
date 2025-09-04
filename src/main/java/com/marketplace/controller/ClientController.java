package com.marketplace.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ClientProfileService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/client")
public class ClientController {

    private final ProfessionalService professionalService;
    private final UserService userService;
    private final ClientProfileService clientProfileService;

    public ClientController(ProfessionalService professionalService, UserService userService , ClientProfileService clientProfileService) {
        this.professionalService = professionalService;
        this.userService = userService;
        this.clientProfileService = clientProfileService;
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
    
    @GetMapping("/dashboard")
    public String clientDashboard(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            boolean hasProfile = clientProfileService.hasProfile(currentUser);
            model.addAttribute("hasProfile", hasProfile);
            
            if (hasProfile) {
                // Add review count or other profile-related data
                // model.addAttribute("reviewCount", getReviewCount(currentUser));
            }
            
            return "client/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "client/dashboard";
        }
    }

  
}