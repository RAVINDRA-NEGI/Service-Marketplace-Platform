package com.marketplace.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.marketplace.dto.ConversationDto;
import com.marketplace.dto.MessageDto;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.service.ClientProfileService;
import com.marketplace.service.ConversationService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.util.MessagingConstants;

@Controller
@RequestMapping("/messaging")
public class MessagingWebController {

    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private ClientProfileService clientProfileService;
    
    @Autowired
    private ProfessionalService professionalProfileService;

    // View all conversations page
    @GetMapping
    public String conversationsPage(Model model, 
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationDto> conversations;
        
        // Determine if user is client or professional
        ClientProfile clientProfile = clientProfileService.findByUser(currentUser);
        if (clientProfile != null) {
            conversations = conversationService.getConversationsByClient(clientProfile, "active", pageable);
        } else {
            ProfessionalProfile professionalProfile = professionalProfileService.findByUser(currentUser);
            if (professionalProfile != null) {
                conversations = conversationService.getConversationsByProfessional(professionalProfile, "active", pageable);
            } else {
                model.addAttribute("error", "User profile not found");
                return "error";
            }
        }
        
        model.addAttribute("conversations", conversations);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", conversations.getTotalPages());
        model.addAttribute("quickReplies", MessagingConstants.QUICK_REPLIES);
        
        return "messaging/conversations";
    }

    // View specific conversation page
    @GetMapping("/conversation/{conversationId}")
    public String conversationPage(@PathVariable Long conversationId, Model model) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        ConversationDto conversation = conversationService.getConversationByIdAndUserDto(
            conversationId, currentUser);
        
        if (conversation == null) {
            model.addAttribute("error", MessagingConstants.ERROR_ACCESS_DENIED);
            return "error";
        }

        // Load recent messages
        Pageable pageable = PageRequest.of(0, 50);
        Page<MessageDto> messages = conversationService.getMessagesByConversation(
            conversationId, currentUser, pageable);

        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages.getContent());
        model.addAttribute("quickReplies", MessagingConstants.QUICK_REPLIES);
        
        // Mark conversation as read
        conversationService.markConversationAsRead(conversationId, currentUser);
        
        return "messaging/conversation";
    }

    // Create new conversation page
    @GetMapping("/new")
    public String newConversationPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Load recent conversations based on user type
        List<ConversationDto> recentConversations;
        ClientProfile clientProfile = clientProfileService.findByUser(currentUser);
        if (clientProfile != null) {
            recentConversations = conversationService.getRecentConversations(clientProfile);
        } else {
            ProfessionalProfile professionalProfile = professionalProfileService.findByUser(currentUser);
            if (professionalProfile != null) {
                recentConversations = conversationService.getRecentConversations(professionalProfile);
            } else {
                model.addAttribute("error", "User profile not found");
                return "error";
            }
        }
        
        model.addAttribute("recentConversations", recentConversations);
        model.addAttribute("quickReplies", MessagingConstants.QUICK_REPLIES);
        
        return "messaging/new-conversation";
    }

    // View conversation list for client
    @GetMapping("/client")
    public String clientConversationsPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        ClientProfile clientProfile = clientProfileService.findByUser(currentUser);
        if (clientProfile == null) {
            model.addAttribute("error", "Client profile not found");
            return "error";
        }

        Pageable pageable = PageRequest.of(0, 20);
        Page<ConversationDto> conversations = conversationService.getConversationsByClient(
            clientProfile, "active", pageable);
        
        model.addAttribute("conversations", conversations.getContent());
        model.addAttribute("userType", "client");
        model.addAttribute("quickReplies", MessagingConstants.QUICK_REPLIES);
        
        return "messaging/client-conversations";
    }

    // View conversation list for professional
    @GetMapping("/professional")
    public String professionalConversationsPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        ProfessionalProfile professionalProfile = professionalProfileService.findByUser(currentUser);
        if (professionalProfile == null) {
            model.addAttribute("error", "Professional profile not found");
            return "error";
        }

        Pageable pageable = PageRequest.of(0, 20);
        Page<ConversationDto> conversations = conversationService.getConversationsByProfessional(
            professionalProfile, "active", pageable);
        
        model.addAttribute("conversations", conversations.getContent());
        model.addAttribute("userType", "professional");
        model.addAttribute("quickReplies", MessagingConstants.QUICK_REPLIES);
        
        return "messaging/professional-conversations";
    }

    // Helper method to get current user
    private User getCurrentUser() {
        // This would use your existing authentication mechanism
        // For example: get from SecurityContextHolder
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }
}