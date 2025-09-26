package com.marketplace.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.marketplace.security.service.UserDetailsImpl;

@Component
public class WebSocketUserInterceptor implements ChannelInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketUserInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract user from HTTP session or JWT token
            Authentication user = extractUserFromSession(accessor);
            if (user != null) {
                accessor.setUser(user);
                logger.debug("User {} connected to WebSocket", user.getName());
            } else {
                logger.warn("WebSocket connection attempt without valid authentication");
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // Log subscription for monitoring
            String destination = accessor.getDestination();
            Authentication user = (Authentication) accessor.getUser();
            if (user != null && destination != null) {
                logger.info("User {} subscribed to {}", user.getName(), destination);
            }
        }
    }

    private Authentication extractUserFromSession(StompHeaderAccessor accessor) {
        try {
            // Method 1: Extract from session attributes
            if (accessor.getSessionAttributes() != null) {
                Object principal = accessor.getSessionAttributes().get("user");
                if (principal instanceof UserDetailsImpl) {
                    UserDetailsImpl userDetails = (UserDetailsImpl) principal;
                    return new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                }
            }

            // Method 2: Extract from native headers (JWT token)
            String authToken = accessor.getFirstNativeHeader("Authorization");
            if (authToken != null && authToken.startsWith("Bearer ")) {
                // Parse JWT token here and create Authentication
                return parseJwtToken(authToken.substring(7));
            }

            // Method 3: Extract from session ID if available
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                // Look up user by session ID from your session store
                return getUserBySessionId(sessionId);
            }

        } catch (Exception e) {
            logger.error("Error extracting user from WebSocket session", e);
        }
        
        return null;
    }

    // Placeholder method for JWT parsing
    private Authentication parseJwtToken(String token) {
        // Implement JWT token parsing logic here
        // This would typically involve validating the JWT and extracting user details
        // Return null for now - implement based on your JWT setup
        return null;
    }

    // Placeholder method for session-based lookup
    private Authentication getUserBySessionId(String sessionId) {
        // Implement session-based user lookup here
        // This would query your session store or database
        return null;
    }
}