package com.marketplace.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.marketplace.dto.OnlineStatusMessage;
import com.marketplace.service.impl.WebSocketMessageService;

@Component
public class WebSocketDisconnectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketDisconnectHandler.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String sessionId = headerAccessor.getSessionId();
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (username != null) {
            logger.info("User disconnected: {} with session ID: {}", username, sessionId);
            
            // Update user online status
            webSocketMessageService.updateUserOnlineStatus(username, false);
            
            // Send offline notification to relevant conversations
            messagingTemplate.convertAndSend("/topic/online-status", 
                new OnlineStatusMessage(username, false));
        }
    }
}