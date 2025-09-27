package com.marketplace.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.dto.ConversationDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.User;
import com.marketplace.service.ConversationService;
import com.marketplace.util.MessagingConstants;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    // Get all conversations for current user
    @GetMapping
    public ResponseEntity<?> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "active") String status,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationDto> conversations = conversationService.getConversationsByUser(currentUser, status, pageable);

            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get specific conversation
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            ConversationDto conversation = conversationService.getConversationByIdAndUserDto(conversationId, currentUser);
            if (conversation == null) {
                return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_ACCESS_DENIED));
            }

            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Create new conversation (or get existing one)
    @PostMapping
    public ResponseEntity<?> createConversation(
            @RequestParam Long professionalId,
            @RequestParam(required = false) Long bookingId,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            ConversationDto conversation = conversationService.createOrGetConversation(
                currentUser, professionalId, bookingId);
            
            return ResponseEntity.ok(Map.of(
                "message", MessagingConstants.SUCCESS_CONVERSATION_CREATED,
                "data", conversation
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Mark conversation as read
    @PostMapping("/{conversationId}/read")
    public ResponseEntity<?> markConversationAsRead(
            @PathVariable Long conversationId,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            conversationService.markConversationAsRead(conversationId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Conversation marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Close conversation
    @PostMapping("/{conversationId}/close")
    public ResponseEntity<?> closeConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            Conversation conversation = conversationService.getConversationByIdAndUser(conversationId, currentUser);
            if (conversation == null) {
                return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_ACCESS_DENIED));
            }

            conversationService.closeConversation(conversationId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Conversation closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get online status for a user
    @GetMapping("/user/{userId}/online")
    public ResponseEntity<?> getUserOnlineStatus(@PathVariable Long userId) {
        boolean isOnline = conversationService.isUserOnline(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "online", isOnline));
    }

    // Get conversation statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getConversationStatistics(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            Map<String, Object> stats = conversationService.getConversationStatistics(currentUser);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}