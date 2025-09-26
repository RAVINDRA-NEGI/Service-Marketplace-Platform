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
            // Get current user
            User currentUser = webSocketMessageService.getCurrentUser();
            if (currentUser == null) {
                return;
            }

            // Validate conversation access
            Conversation conversation = conversationService.getConversationByIdAndUser(conversationId, currentUser);
            if (conversation == null) {
                return;
            }

            // Process and save message
            Message savedMessage = conversationService.processSendMessage(sendMessageDto, currentUser, conversation);

            // Convert to DTO and send via WebSocket
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
            // Handle error
            messagingTemplate.convertAndSendToUser(
                headerAccessor.getSessionId(),
                "/queue/errors",
                "Error sending message: " + e.getMessage()
            );
        }
    }

    @MessageMapping("/chat/{conversationId}/typing")
    public void sendTypingIndicator(@DestinationVariable Long conversationId,
                                   @Payload boolean isTyping,
                                   SimpMessageHeaderAccessor headerAccessor) {
        
        User currentUser = webSocketMessageService.getCurrentUser();
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
        
        User currentUser = webSocketMessageService.getCurrentUser();
        if (currentUser != null) {
            webSocketMessageService.markMessageAsRead(messageId, conversationId, currentUser);
        }
    }
}