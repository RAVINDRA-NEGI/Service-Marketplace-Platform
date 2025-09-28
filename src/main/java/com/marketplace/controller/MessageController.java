package com.marketplace.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.marketplace.dto.ChatMessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ConversationService;
import com.marketplace.service.MessageService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserService userService;

    public MessageController(MessageService messageService,
                           ConversationService conversationService,
                           UserService userService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
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

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageDto sendMessageDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            Conversation conversation = conversationService.getConversationById(sendMessageDto.getConversationId());
            
            if (!conversationService.hasAccessToConversation(conversation, currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            Message savedMessage = messageService.createMessage(conversation, currentUser, sendMessageDto);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageId", savedMessage.getId(),
                "createdAt", savedMessage.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversation/{conversationId}/history")
    @ResponseBody
    public ResponseEntity<?> getConversationHistory(@PathVariable Long conversationId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            Conversation conversation = conversationService.getConversationById(conversationId);
            
            if (!conversationService.hasAccessToConversation(conversation, currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            List<Message> messages = messageService.getRecentMessages(conversation, page * size, size);
            
            // Convert to DTOs
            List<ChatMessageDto> messageDtos = messages.stream()
                .map(msg -> convertToChatMessageDto(msg, currentUser))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "messages", messageDtos,
                "total", messageService.countMessagesByConversation(conversation),
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/conversation/{conversationId}/mark-as-read")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long conversationId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            Conversation conversation = conversationService.getConversationById(conversationId);
            
            if (!conversationService.hasAccessToConversation(conversation, currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            messageService.markMessagesAsRead(conversation, currentUser);
            conversationService.resetUnreadCount(conversation, currentUser);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{messageId}")
    @ResponseBody
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            Message message = messageService.getMessageById(messageId);
            
            // Only allow sender to delete their own messages (within time limit)
            if (!message.isFromUser(currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            // Check if message is too old to delete (e.g., within 1 hour)
            if (message.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusHours(1))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message too old to delete"));
            }

            messageService.deleteMessage(message);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

 // Updated MessageController.java - getAvatarUrl method
    private String getAvatarUrl(User user, Conversation conversation) {
        // Check if user is client in this conversation
        if (conversation.getClient().getUser().getId().equals(user.getId())) {
            return conversation.getClient().getProfilePhotoUrl();
        }
        
        // Check if user is professional in this conversation
        if (conversation.getProfessional().getUser().getId().equals(user.getId())) {
            return conversation.getProfessional().getProfilePhotoUrl();
        }
        
        // Default avatar if none found
        return "/images/default-avatar.png";
    }

    // Updated convertToChatMessageDto method:
    private ChatMessageDto convertToChatMessageDto(Message message, User currentUser) {
        // Get the conversation to determine which profile to use
        Conversation conversation = message.getConversation();
        
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(String.valueOf(message.getId()));
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setSenderAvatarUrl(getAvatarUrl(message.getSender(), conversation));
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setMessageStatus(message.getMessageStatus());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setReadAt(message.getReadAt());
        dto.setIsFromCurrentUser(message.isFromUser(currentUser));
        
        return dto;
    }
}