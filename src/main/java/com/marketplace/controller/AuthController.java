package com.marketplace.controller;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.UserRegistrationDto;
import com.marketplace.exception.UserAlreadyExistsException;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.User;
import com.marketplace.service.UserService;

import jakarta.validation.Valid;

@Controller

@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

   
    private final UserService userService;
   

    public AuthController( UserService userService) {
     
        this.userService = userService;
     
    }


    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           Model model) {
        if ("true".equals(error)) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }

    @GetMapping("/register/client")
    public String showClientRegistrationForm(Model model) {
        if (!model.containsAttribute("userRegistrationDto")) {
            model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        }
        return "register-client";
    }

    @GetMapping("/register/professional")
    public String showProfessionalRegistrationForm(Model model) {
        if (!model.containsAttribute("userRegistrationDto")) {
            model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        }
        return "register-professional";
    }

    @PostMapping("/register/client")
    public String registerClient(@Valid @ModelAttribute("userRegistrationDto") UserRegistrationDto registrationDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            return "register-client";
        }

        try {
            User user = userService.registerClient(registrationDto);
            redirectAttributes.addFlashAttribute("message", "Client registered successfully! Please login.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("userRegistrationDto", registrationDto);
            result.rejectValue("email", "error.user", e.getMessage());
            return "register-client";
        } catch (UsernameTakenException e) {
            model.addAttribute("userRegistrationDto", registrationDto);
            result.rejectValue("username", "error.user", e.getMessage());
            return "register-client";
        }
    }

    @PostMapping("/register/professional")
    public String registerProfessional(@Valid @ModelAttribute("userRegistrationDto") UserRegistrationDto registrationDto,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (result.hasErrors()) {
            return "register-professional";
        }

        try {
            User user = userService.registerProfessional(registrationDto);
            redirectAttributes.addFlashAttribute("message", "Professional registered successfully! Please login.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("userRegistrationDto", registrationDto);
            result.rejectValue("email", "error.user", e.getMessage());
            return "register-professional";
        } catch (UsernameTakenException e) {
            model.addAttribute("userRegistrationDto", registrationDto);
            result.rejectValue("username", "error.user", e.getMessage());
            return "register-professional";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CLIENT"))) {
            return "redirect:/client/dashboard";
        } else if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PROFESSIONAL"))) {
            return "redirect:/professional/dashboard";
        }
        
        return "redirect:/";
    }

    @GetMapping("/client/dashboard")
    public String clientDashboard() {
        return "client/dashboard";
    }

    @GetMapping("/professional/dashboard")
    public String professionalDashboard() {
        return "professional/dashboard";
    }
}