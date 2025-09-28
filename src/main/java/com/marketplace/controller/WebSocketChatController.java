package com.marketplace.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.marketplace.dto.ChatMessageDto;
import com.marketplace.dto.MessageFileDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;
import com.marketplace.model.User;
import com.marketplace.service.ClientProfileService;
import com.marketplace.service.ConversationService;
import com.marketplace.service.MessageService;
import com.marketplace.service.ProfessionalService;
import com.marketplace.service.UserService;

@Controller
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ClientProfileService clientProfileService;
    private final ProfessionalService professionalService;
    private final UserService userService;

    public WebSocketChatController(SimpMessageSendingOperations messagingTemplate,
                                 ConversationService conversationService,
                                 MessageService messageService,
                                 ClientProfileService clientProfileService,
                                 ProfessionalService professionalService,
                                 UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.clientProfileService = clientProfileService;
        this.professionalService = professionalService;
        this.userService = userService;
    }

    @MessageMapping("/chat/send/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId, 
                           @Payload SendMessageDto sendMessageDto,
                           Authentication authentication) {
        try {
            User sender = (User) authentication.getPrincipal();
            
            // Validate conversation access
            Conversation conversation = conversationService.getConversationById(conversationId);
            if (!hasAccessToConversation(conversation, sender)) {
                throw new RuntimeException("Access denied to conversation");
            }

            // Create and save message
            Message savedMessage = messageService.createMessage(conversation, sender, sendMessageDto);
            
            // Convert to DTO for WebSocket
            ChatMessageDto messageDto = convertToChatMessageDto(savedMessage, sender);
            
            // Send to conversation topic (both participants)
            String conversationTopic = "/topic/conversation/" + conversationId;
            messagingTemplate.convertAndSend(conversationTopic, messageDto);
            
            // Send private notification to other participant
            User otherParticipant = getOtherParticipant(conversation, sender);
            String privateNotification = "/user/queue/conversation/" + conversationId + "/notification";
            messagingTemplate.convertAndSendToUser(
                String.valueOf(otherParticipant.getId()), 
                "/queue/conversation/" + conversationId + "/notification", 
                messageDto
            );
            
            logger.info("Message sent successfully to conversation: {}", conversationId);
            
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage());
            
            // Send error message back to sender
            ChatMessageDto errorDto = new ChatMessageDto();
            errorDto.setError("Failed to send message: " + e.getMessage());
            errorDto.setSuccess(false);
            errorDto.setConversationId(conversationId);
            
            messagingTemplate.convertAndSendToUser(
                String.valueOf(((User) authentication.getPrincipal()).getId()),
                "/queue/errors",
                errorDto
            );
        }
    }

    @MessageMapping("/chat/typing/{conversationId}")
    public void sendTypingIndicator(@DestinationVariable Long conversationId, 
                                   Authentication authentication) {
        try {
            User sender = (User) authentication.getPrincipal();
            Conversation conversation = conversationService.getConversationById(conversationId);
            
            if (!hasAccessToConversation(conversation, sender)) {
                return;
            }

            String typingIndicator = sender.getFullName() + " is typing...";
            String typingTopic = "/topic/conversation/" + conversationId + "/typing";
            
            messagingTemplate.convertAndSend(typingTopic, 
                Map.of("conversationId", conversationId, 
                       "typingUser", sender.getFullName(),
                       "typing", true));
        } catch (Exception e) {
            logger.error("Error sending typing indicator: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat/mark-as-read/{conversationId}")
    public void markAsRead(@DestinationVariable Long conversationId, 
                          Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Conversation conversation = conversationService.getConversationById(conversationId);
            
            if (!hasAccessToConversation(conversation, user)) {
                return;
            }

            // Mark messages as read
            messageService.markMessagesAsRead(conversation, user);
            
            // Update conversation unread count
            conversationService.resetUnreadCount(conversation, user);
            
            // Send read receipt to other participant
            User otherParticipant = getOtherParticipant(conversation, user);
            String readReceiptTopic = "/topic/conversation/" + conversationId + "/read-receipt";
            messagingTemplate.convertAndSendToUser(
                String.valueOf(otherParticipant.getId()),
                "/queue/conversation/" + conversationId + "/read-receipt",
                Map.of("conversationId", conversationId, "readByUserId", user.getId())
            );
            
        } catch (Exception e) {
            logger.error("Error marking messages as read: {}", e.getMessage());
        }
    }

    // Helper methods
    private boolean hasAccessToConversation(Conversation conversation, User user) {
        return conversation.getClient().getUser().getId().equals(user.getId()) ||
               conversation.getProfessional().getUser().getId().equals(user.getId());
    }

    private User getOtherParticipant(Conversation conversation, User currentUser) {
        if (conversation.getClient().getUser().getId().equals(currentUser.getId())) {
            return conversation.getProfessional().getUser();
        } else {
            return conversation.getClient().getUser();
        }
    }

 // Updated WebSocketChatController.java - getAvatarUrl method
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

    // Updated convertToChatMessageDto method in WebSocket controller:
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
        
        // Convert files if any
        if (message.getFiles() != null && !message.getFiles().isEmpty()) {
            List<MessageFileDto> fileDtos = message.getFiles().stream()
                .map(this::convertToFileDto)
                .collect(Collectors.toList());
            dto.setFiles(fileDtos);
        }
        
        return dto;
    }

    private MessageFileDto convertToFileDto(MessageFile file) {
        MessageFileDto dto = new MessageFileDto();
        dto.setId(file.getId());
        dto.setOriginalFilename(file.getOriginalFilename());
        dto.setStoredFilename(file.getStoredFilename());
        dto.setFileUrl(file.getFileUrl());
        dto.setFileSize(file.getFileSize());
        dto.setContentType(file.getContentType());
        dto.setFileType(file.getFileType());
        dto.setIconClass(getIconClassForFileType(file.getFileType()));
        dto.setThumbnailUrl(file.getFileUrl()); // For images, you might have separate thumbnail
        return dto;
    }
    

    private String getIconClassForFileType(String fileType) {
        switch (fileType) {
            case "photo": return "fas fa-image";
            case "document": return "fas fa-file-pdf";
            case "video": return "fas fa-video";
            case "audio": return "fas fa-music";
            default: return "fas fa-file";
        }
    }
}
