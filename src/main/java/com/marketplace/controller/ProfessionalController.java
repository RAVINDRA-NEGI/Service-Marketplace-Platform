package com.marketplace.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.ProfessionalProfileDto;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.ServiceCategory;
import com.marketplace.model.User;
import com.marketplace.service.ProfessionalService;
import com.marketplace.util.Constants;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/professional")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    public ProfessionalController(ProfessionalService professionalService) {
        this.professionalService = professionalService;
    }

    @GetMapping("/profile/setup")
    public String showProfileSetupForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        if (professionalService.hasProfile(currentUser)) {
            return "redirect:/professional/profile";
        }

        if (!model.containsAttribute("profileDto")) {
            model.addAttribute("profileDto", new ProfessionalProfileDto());
        }

        List<ServiceCategory> categories = professionalService.getAllCategories();
        model.addAttribute("categories", categories);
        return "professional/profile-setup";
    }

    @PostMapping("/profile/setup")
    public String createProfile(@Valid @ModelAttribute("profileDto") ProfessionalProfileDto profileDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        if (result.hasErrors()) {
            List<ServiceCategory> categories = professionalService.getAllCategories();
            model.addAttribute("categories", categories);
            return "professional/profile-setup";
        }

        try {
            ProfessionalProfile profile = professionalService.createProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", Constants.PROFILE_CREATED_SUCCESS);
            return "redirect:/professional/profile";
        } catch (Exception e) {
            List<ServiceCategory> categories = professionalService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("error", e.getMessage());
            return "professional/profile-setup";
        }
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        try {
            ProfessionalProfile profile = professionalService.getProfileByUser(currentUser);
            model.addAttribute("profile", profile);
            return "professional/profile-view";
        } catch (Exception e) {
            return "redirect:/professional/profile/setup";
        }
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        try {
            ProfessionalProfile profile = professionalService.getProfileByUser(currentUser);
            ProfessionalProfileDto profileDto = new ProfessionalProfileDto();
            profileDto.setId(profile.getId());
            profileDto.setBio(profile.getBio());
            profileDto.setCertification(profile.getCertification());
            profileDto.setProfilePhotoUrl(profile.getProfilePhotoUrl());
            profileDto.setCategoryId(profile.getCategory().getId());
            profileDto.setHourlyRate(profile.getHourlyRate());
            profileDto.setServiceAreaCity(profile.getServiceAreaCity());
            profileDto.setServiceAreaState(profile.getServiceAreaState());

            model.addAttribute("profileDto", profileDto);
            model.addAttribute("categories", professionalService.getAllCategories());
            return "professional/profile-edit";
        } catch (Exception e) {
            return "redirect:/professional/profile/setup";
        }
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("profileDto") ProfessionalProfileDto profileDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        if (result.hasErrors()) {
            model.addAttribute("categories", professionalService.getAllCategories());
            return "professional/profile-edit";
        }

        try {
            ProfessionalProfile profile = professionalService.updateProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", Constants.PROFILE_UPDATED_SUCCESS);
            return "redirect:/professional/profile";
        } catch (Exception e) {
            model.addAttribute("categories", professionalService.getAllCategories());
            model.addAttribute("error", e.getMessage());
            return "professional/profile-edit";
        }
    }

    @GetMapping("/availability")
    public String manageAvailability(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        try {
            ProfessionalProfile profile = professionalService.getProfileByUser(currentUser);
            List<Availability> availability = professionalService.getAvailability(profile.getId());
            model.addAttribute("profile", profile);
            model.addAttribute("availability", availability);
            return "professional/availability";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "professional/availability";
        }
    }
}