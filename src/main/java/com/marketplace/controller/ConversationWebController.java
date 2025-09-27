package com.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.dto.ConversationDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.User;
import com.marketplace.service.ConversationService;
import com.marketplace.util.MessagingConstants;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/conversation")
public class ConversationWebController {

    @Autowired
    private ConversationService conversationService;

    // Send message via form submission (for Thymeleaf)
    @PostMapping("/{conversationId}/send")
    public String sendMessage(@PathVariable Long conversationId,
                             @Valid @ModelAttribute("sendMessageDto") SendMessageDto sendMessageDto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please correct the errors below");
            return "redirect:/messaging/conversation/" + conversationId;
        }

        try {
            Conversation conversation = conversationService.getConversationByIdAndUser(
                conversationId, currentUser);
            
            if (conversation == null) {
                redirectAttributes.addFlashAttribute("error", MessagingConstants.ERROR_ACCESS_DENIED);
                return "redirect:/messaging";
            }

            // Process message (this would call the service layer)
            // For now, we'll redirect and let JavaScript handle the WebSocket
            redirectAttributes.addFlashAttribute("message", MessagingConstants.SUCCESS_MESSAGE_SENT);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/messaging/conversation/" + conversationId;
    }

    // Create new conversation
    @PostMapping("/create")
    public String createConversation(@RequestParam Long professionalId,
                                   @RequestParam(required = false) Long bookingId,
                                   RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            ConversationDto conversation = conversationService.createOrGetConversation(
                currentUser, professionalId, bookingId);
            
            redirectAttributes.addFlashAttribute("message", MessagingConstants.SUCCESS_CONVERSATION_CREATED);
            return "redirect:/messaging/conversation/" + conversation.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/messaging/new";
        }
    }

    // Mark conversation as read
    @PostMapping("/{conversationId}/mark-read")
    public String markConversationAsRead(@PathVariable Long conversationId,
                                       RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            conversationService.markConversationAsRead(conversationId, currentUser);
            redirectAttributes.addFlashAttribute("message", "Conversation marked as read");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/messaging/conversation/" + conversationId;
    }

    // Close conversation
    @PostMapping("/{conversationId}/close")
    public String closeConversation(@PathVariable Long conversationId,
                                  RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            conversationService.closeConversation(conversationId, currentUser);
            redirectAttributes.addFlashAttribute("message", "Conversation closed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/messaging";
    }

    // Helper method to get current user
    private User getCurrentUser() {
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }
}