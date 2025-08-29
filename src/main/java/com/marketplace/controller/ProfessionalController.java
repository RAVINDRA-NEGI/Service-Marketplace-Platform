package com.marketplace.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.ProfessionalProfileDto;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.ServiceCategory;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;
import com.marketplace.util.Constants;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/professional")
public class ProfessionalController {

    private final ProfessionalService professionalService;
    private final UserService userService;

    public ProfessionalController(ProfessionalService professionalService, UserService userService) {
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

    @GetMapping("/dashboard")
    public String professionalDashboard(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        boolean hasProfile = professionalService.hasProfile(currentUser);
        model.addAttribute("hasProfile", hasProfile);
        return "professional/dashboard";
    }
    
    @GetMapping("/profile/setup")
    public String showProfileSetupForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

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
                               @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                               @RequestParam(value = "certificates", required = false) List<MultipartFile> certificates,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            List<ServiceCategory> categories = professionalService.getAllCategories();
            model.addAttribute("categories", categories);
            return "professional/profile-setup";
        }

        try {
            // Set uploaded files to DTO
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                profileDto.setProfilePhoto(profilePhoto);
            }
            if (certificates != null && !certificates.isEmpty()) {
                profileDto.setCertificates(certificates);
            }

            ProfessionalProfile profile = professionalService.createProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", Constants.PROFILE_CREATED_SUCCESS);
            return "redirect:/professional/profile";
        } catch (IOException e) {
            List<ServiceCategory> categories = professionalService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("error", "File upload failed: " + e.getMessage());
            return "professional/profile-setup";
        } catch (Exception e) {
            List<ServiceCategory> categories = professionalService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("error", e.getMessage());
            return "professional/profile-setup";
        }
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

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
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

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
                               @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                               @RequestParam(value = "certificates", required = false) List<MultipartFile> certificates,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("categories", professionalService.getAllCategories());
            return "professional/profile-edit";
        }

        try {
            // Set uploaded files to DTO
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                profileDto.setProfilePhoto(profilePhoto);
            }
            if (certificates != null && !certificates.isEmpty()) {
                profileDto.setCertificates(certificates);
            }

            ProfessionalProfile profile = professionalService.updateProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("message", Constants.PROFILE_UPDATED_SUCCESS);
            return "redirect:/professional/profile";
        } catch (IOException e) {
            model.addAttribute("categories", professionalService.getAllCategories());
            model.addAttribute("error", "File upload failed: " + e.getMessage());
            return "professional/profile-edit";
        } catch (Exception e) {
            model.addAttribute("categories", professionalService.getAllCategories());
            model.addAttribute("error", e.getMessage());
            return "professional/profile-edit";
        }
    }

    @PostMapping("/profile/photo")
    public String uploadProfilePhoto(@RequestParam("profilePhoto") MultipartFile photoFile,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            professionalService.updateProfilePhoto(currentUser, photoFile);
            redirectAttributes.addFlashAttribute("message", "Profile photo updated successfully!");
            return "redirect:/professional/profile";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload photo: " + e.getMessage());
            return "redirect:/professional/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professional/profile";
        }
    }
    
    @PostMapping("/profile/certificates")
    public String uploadCertificates(@RequestParam("certificates") List<MultipartFile> certificateFiles,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            professionalService.updateCertificates(currentUser, certificateFiles);
            redirectAttributes.addFlashAttribute("message", "Certificates uploaded successfully!");
            return "redirect:/professional/profile";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload certificates: " + e.getMessage());
            return "redirect:/professional/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/professional/profile";
        }
    }


}