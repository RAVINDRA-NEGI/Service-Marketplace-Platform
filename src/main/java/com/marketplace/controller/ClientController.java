package com.marketplace.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.marketplace.model.ProfessionalProfile;
import com.marketplace.service.ProfessionalService;

@Controller
@RequestMapping("/client")
public class ClientController {

    private final ProfessionalService professionalService;

    public ClientController(ProfessionalService professionalService) {
        this.professionalService = professionalService;
    }

    @GetMapping("/professionals")
    public String browseProfessionals(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProfessionalProfile> professionals = professionalService.searchProfessionals(category, city, minRating, pageable);
        
        model.addAttribute("professionals", professionals);
        model.addAttribute("categories", professionalService.getAllCategories());
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentCity", city);
        model.addAttribute("currentMinRating", minRating);
        
        return "client/browse-professionals";
    }

    @GetMapping("/professional/{id}")
    public String viewProfessionalProfile(@PathVariable Long id, Model model) {
        try {
            ProfessionalProfile profile = professionalService.getProfileById(id);
            model.addAttribute("profile", profile);
            return "client/professional-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Professional not found");
            return "error";
        }
    }
}