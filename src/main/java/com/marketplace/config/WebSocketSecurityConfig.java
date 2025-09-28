package com.marketplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class WebSocketSecurityConfig {

    public ChannelInterceptor securityInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && accessor.getCommand() != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand()) ||
                        StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                        StompCommand.SEND.equals(accessor.getCommand())) {
                        
                        // For session-based auth, verify user is authenticated
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                            throw new RuntimeException("User not authenticated");
                        }
                        
                        // Additional security checks can be added here
                        String destination = accessor.getDestination();
                        if (destination != null) {
                            // Check if user has permission to access this destination
                            validateDestination(destination, auth);
                        }
                    }
                }
                
                return message;
            }
        };
    }

    private void validateDestination(String destination, Authentication auth) {
        // Add destination-specific security checks
        if (destination != null) {
            if (destination.startsWith("/app/chat/send")) {
                // Validate that user can send messages
            } else if (destination.startsWith("/user/queue/notifications")) {
                // Validate notification access
            } else if (destination.startsWith("/topic/conversation/")) {
                // Validate conversation access based on user roles
                validateConversationAccess(destination, auth);
            }
        }
    }

    private void validateConversationAccess(String destination, Authentication auth) {
        // Extract conversation ID from destination
        // Check if user has access to this conversation
        // This will be implemented in Phase 3 with conversation validation
    }
}