package com.marketplace.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.ClientProfileDto;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ClientProfileService;
import com.marketplace.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/client/profile")
public class ClientProfileController {

    private final ClientProfileService clientProfileService;
    private final UserService userService;

    public ClientProfileController(ClientProfileService clientProfileService, UserService userService) {
        this.clientProfileService = clientProfileService;
        this.userService = userService;
    }

    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userService.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    @GetMapping("/setup")
    public String showProfileSetupForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (clientProfileService.hasProfile(currentUser)) {
            return "redirect:/client/profile";
        }

        if (!model.containsAttribute("clientProfileDto")) {
            ClientProfileDto profileDto = new ClientProfileDto();
            profileDto.setFullName(currentUser.getFullName());
            profileDto.setEmail(currentUser.getEmail());
            model.addAttribute("clientProfileDto", profileDto);
        }

        return "client/profile-setup";
    }

    @PostMapping("/setup")
    public String createProfile(@Valid @ModelAttribute("clientProfileDto") ClientProfileDto profileDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "client/profile-setup";
        }

        try {
            ClientProfile profile = clientProfileService.createProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", "Profile created successfully!");
            return "redirect:/client/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "client/profile-setup";
        }
    }

    @GetMapping
    public String viewProfile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ClientProfile profile = clientProfileService.getProfileByUser(currentUser);
            model.addAttribute("profile", profile);
            return "client/profile-view";
        } catch (Exception e) {
            return "redirect:/client/profile/setup";
        }
    }

    @GetMapping("/edit")
    public String showEditProfileForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ClientProfile profile = clientProfileService.getProfileByUser(currentUser);
            ClientProfileDto profileDto = new ClientProfileDto();
            profileDto.setId(profile.getId());
            profileDto.setFullName(profile.getFullName());
            profileDto.setPhoneNumber(profile.getPhoneNumber());
            profileDto.setEmail(profile.getEmail());
            profileDto.setAddress(profile.getAddress());
            profileDto.setCity(profile.getCity());
            profileDto.setState(profile.getState());
            profileDto.setBio(profile.getBio());
            profileDto.setProfilePhotoUrl(profile.getProfilePhotoUrl());

            model.addAttribute("clientProfileDto", profileDto);
            return "client/profile-edit";
        } catch (Exception e) {
            return "redirect:/client/profile/setup";
        }
    }

    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute("clientProfileDto") ClientProfileDto profileDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "client/profile-edit";
        }

        try {
            ClientProfile profile = clientProfileService.updateProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
            return "redirect:/client/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "client/profile-edit";
        }
    }

    @PostMapping("/photo")
    public String uploadProfilePhoto(@RequestParam("profilePhoto") MultipartFile photoFile,
                                   RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ClientProfile profile = clientProfileService.updateProfilePhoto(currentUser, photoFile);
            redirectAttributes.addFlashAttribute("message", "Profile photo updated successfully!");
            return "redirect:/client/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/client/profile/edit";
        }
    }
}