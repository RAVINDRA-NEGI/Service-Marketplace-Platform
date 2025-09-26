package com.marketplace.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.marketplace.service.WebSocketMessageService;

@Component
public class WebSocketConnectHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectHandler.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private WebSocketMessageService webSocketMessageService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String sessionId = headerAccessor.getSessionId();
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        
        if (username != null) {
            logger.info("User connected: {} with session ID: {}", username, sessionId);
            
            // Update user online status
            webSocketMessageService.updateUserOnlineStatus(username, true);
            
            // Send online notification to relevant conversations
            messagingTemplate.convertAndSend("/topic/online-status", 
                new OnlineStatusMessage(username, true));
        }
    }
}