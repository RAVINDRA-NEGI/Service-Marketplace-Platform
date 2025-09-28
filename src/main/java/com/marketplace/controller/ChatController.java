package com.marketplace.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.marketplace.dto.ChatMessageDto;
import com.marketplace.dto.ConversationDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.ConversationService;
import com.marketplace.service.MessageService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;

    public ChatController(ConversationService conversationService,
                         MessageService messageService,
                         UserService userService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
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

    @GetMapping("/conversations")
    public String getConversations(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Get conversations for current user (either as client or professional)
            List<Conversation> conversations = conversationService.getConversationsForUser(currentUser);
            
            // Convert to DTOs for Thymeleaf template
            List<ConversationDto> conversationDtos = conversations.stream()
                .map(conv -> convertToConversationDto(conv, currentUser))
                .collect(Collectors.toList());
            
            model.addAttribute("conversations", conversationDtos);
            return "chat/conversations";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "chat/conversations";
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public String getConversation(@PathVariable Long conversationId, 
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Conversation conversation = conversationService.getConversationById(conversationId);
            
            // Verify user has access to this conversation
            if (!conversationService.hasAccessToConversation(conversation, currentUser)) {
                model.addAttribute("error", "Access denied to conversation");
                return "error";
            }

            // Get paginated messages
            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messagesPage = messageService.getMessagesByConversation(conversation, pageable);
            
            // Convert messages to DTOs
            List<ChatMessageDto> messageDtos = messagesPage.getContent().stream()
                .map(msg -> convertToChatMessageDto(msg, currentUser))
                .collect(Collectors.toList());
            
            // Reset unread count for current user
            conversationService.resetUnreadCount(conversation, currentUser);
            
            // Get conversation details
            ConversationDto conversationDto = convertToConversationDto(conversation, currentUser);
            
            model.addAttribute("conversation", conversationDto);
            model.addAttribute("messages", messageDtos);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", messagesPage.getTotalPages());
            model.addAttribute("hasPrevious", page > 0);
            model.addAttribute("hasNext", page < messagesPage.getTotalPages() - 1);
            
            return "chat/conversation";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "chat/conversations";
        }
    }

    @GetMapping("/conversation/{conversationId}/messages")
    @ResponseBody
    public List<ChatMessageDto> getMessages(@PathVariable Long conversationId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        Conversation conversation = conversationService.getConversationById(conversationId);
        
        if (!conversationService.hasAccessToConversation(conversation, currentUser)) {
            throw new RuntimeException("Access denied");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage = messageService.getMessagesByConversation(conversation, pageable);
        
        return messagesPage.getContent().stream()
            .map(msg -> convertToChatMessageDto(msg, currentUser))
            .collect(Collectors.toList());
    }

 // Updated ChatController.java - getAvatarUrl method
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

    // Updated convertToConversationDto method:
    private ConversationDto convertToConversationDto(Conversation conversation, User currentUser) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        
        // Client info
        dto.setClientId(conversation.getClient().getUser().getId());
        dto.setClientName(conversation.getClient().getUser().getFullName());
        dto.setClientAvatarUrl(getAvatarUrl(conversation.getClient().getUser(), conversation));
        
        // Professional info
        dto.setProfessionalId(conversation.getProfessional().getUser().getId());
        dto.setProfessionalName(conversation.getProfessional().getUser().getFullName());
        dto.setProfessionalAvatarUrl(getAvatarUrl(conversation.getProfessional().getUser(), conversation));
        
        dto.setBookingId(conversation.getBooking() != null ? conversation.getBooking().getId() : null);
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageType(conversation.getLastMessageType());
        dto.setLastMessageSentAt(conversation.getLastMessageSentAt());
        
        // Set unread count based on current user
        if (conversation.getClient().getUser().getId().equals(currentUser.getId())) {
            dto.setUnreadCount(conversation.getUnreadCountClient());
        } else {
            dto.setUnreadCount(conversation.getUnreadCountProfessional());
        }
        
        dto.setIsActive(conversation.getIsActive());
        dto.setIsClosed(conversation.getIsClosed());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setIsOnline(false); // Will be updated by WebSocket events
        
        return dto;
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
        
        // Add date separator for grouping
        if (message.getCreatedAt() != null) {
            dto.setDateSeparator(message.getCreatedAt().toLocalDate().toString());
        }
        
        return dto;
    }
}