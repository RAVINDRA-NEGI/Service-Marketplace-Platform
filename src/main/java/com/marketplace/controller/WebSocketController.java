package com.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import com.marketplace.dto.MessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.service.ConversationService;
import com.marketplace.service.impl.WebSocketMessageService;
import com.marketplace.util.MessagingConstants;

@Controller
public class WebSocketController {

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ConversationService conversationService;

    @MessageMapping("/chat/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId, 
                           @Payload SendMessageDto sendMessageDto,
                           SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            // Get current user from session
            User currentUser = getCurrentUserFromSession(headerAccessor);
            if (currentUser == null) {
                sendErrorToUser(headerAccessor, "Authentication required");
                return;
            }

            // Validate conversation access
            Conversation conversation = conversationService.getConversationByIdAndUser(conversationId, currentUser);
            if (conversation == null) {
                sendErrorToUser(headerAccessor, MessagingConstants.ERROR_ACCESS_DENIED);
                return;
            }

            // Process and save message
            Message savedMessage = conversationService.processSendMessage(sendMessageDto, currentUser, conversation);
            MessageDto messageDto = conversationService.convertToMessageDto(savedMessage, currentUser);

            // Send to conversation topic
            webSocketMessageService.sendMessage(conversationId, messageDto);

            // Update conversation last message
            webSocketMessageService.updateConversationLastMessage(
                conversationId, 
                sendMessageDto.getContent(), 
                currentUser
            );

        } catch (Exception e) {
            sendErrorToUser(headerAccessor, "Error sending message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat/{conversationId}/typing")
    public void sendTypingIndicator(@DestinationVariable Long conversationId,
                                   @Payload boolean isTyping,
                                   SimpMessageHeaderAccessor headerAccessor) {
        
        User currentUser = getCurrentUserFromSession(headerAccessor);
        if (currentUser != null) {
            webSocketMessageService.sendTypingIndicator(
                conversationId, 
                currentUser.getFullName(), 
                isTyping
            );
        }
    }

    @MessageMapping("/chat/{conversationId}/read/{messageId}")
    public void markAsRead(@DestinationVariable Long conversationId,
                          @DestinationVariable Long messageId,
                          SimpMessageHeaderAccessor headerAccessor) {
        
        User currentUser = getCurrentUserFromSession(headerAccessor);
        if (currentUser != null) {
            webSocketMessageService.markMessageAsRead(messageId, conversationId, currentUser);
        }
    }

    // Send error to user
    private void sendErrorToUser(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
            headerAccessor.getSessionId(),
            MessagingConstants.WS_USER_QUEUE_ERRORS,
            errorMessage
        );
    }

    // Get current user from WebSocket session
    private User getCurrentUserFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object user = headerAccessor.getUser();
        if (user instanceof org.springframework.security.core.Authentication) {
            return (User) ((org.springframework.security.core.Authentication) user).getPrincipal();
        }
        return null;
    }
}