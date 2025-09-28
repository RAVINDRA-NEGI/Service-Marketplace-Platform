package com.marketplace.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketDisconnectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketDisconnectHandler.class);

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketDisconnectHandler(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        
        if (username != null) {
            logger.info("User {} (ID: {}) disconnected", username, userId);
            
            // Update user status to offline
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/status", 
                Map.of("status", "offline", "username", username, "userId", userId));
            
            // Send disconnect notification
            messagingTemplate.convertAndSend("/topic/disconnect", 
                Map.of("username", username, "userId", userId, "status", "disconnected"));
        }
    }
}