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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.dto.MessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.service.ConversationService;
import com.marketplace.service.impl.WebSocketMessageService;
import com.marketplace.util.MessagingConstants;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messaging")
public class MessagingController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    // Get messages for a conversation
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            // Validate access to conversation
            Conversation conversation = conversationService.getConversationByIdAndUser(conversationId, currentUser);
            if (conversation == null) {
                return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_ACCESS_DENIED));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<MessageDto> messages = conversationService.getMessagesByConversation(conversationId, currentUser, pageable);

            // Mark conversation as read
            conversationService.markConversationAsRead(conversationId, currentUser);

            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Send a new message
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageDto sendMessageDto,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            // Validate access to conversation
            Conversation conversation = conversationService.getConversationByIdAndUser(conversationId, currentUser);
            if (conversation == null) {
                return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_ACCESS_DENIED));
            }

            // Process and save message
            Message savedMessage = conversationService.processSendMessage(sendMessageDto, currentUser, conversation);
            MessageDto messageDto = conversationService.convertToMessageDto(savedMessage, currentUser);

            // Send via WebSocket
            webSocketMessageService.sendMessage(conversationId, messageDto);

            // Update conversation last message
            webSocketMessageService.updateConversationLastMessage(
                conversationId, 
                sendMessageDto.getContent(), 
                currentUser
            );

            return ResponseEntity.ok(Map.of(
                "message", MessagingConstants.SUCCESS_MESSAGE_SENT,
                "data", messageDto
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Mark message as read
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<?> markMessageAsRead(
            @PathVariable Long messageId,
            @RequestParam Long conversationId,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            webSocketMessageService.markMessageAsRead(messageId, conversationId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Message marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get unread message count for user
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            Map<String, Object> unreadCounts = conversationService.getUnreadCountsForUser(currentUser);
            return ResponseEntity.ok(unreadCounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Search messages in conversation
    @GetMapping("/conversations/{conversationId}/messages/search")
    public ResponseEntity<?> searchMessages(
            @PathVariable Long conversationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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

            Pageable pageable = PageRequest.of(page, size);
            Page<MessageDto> searchResults = conversationService.searchMessages(conversationId, query, pageable);

            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get quick reply suggestions
    @GetMapping("/quick-replies")
    public ResponseEntity<?> getQuickReplies() {
        return ResponseEntity.ok(MessagingConstants.QUICK_REPLIES);
    }

    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            // In a real implementation, you'd fetch from database using username
            // For now, returning null - you'd need to implement proper user fetching
        }
        return (User) authentication.getPrincipal();
    }
}