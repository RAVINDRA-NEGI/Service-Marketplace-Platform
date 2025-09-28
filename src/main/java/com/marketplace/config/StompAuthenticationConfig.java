package com.marketplace.config;


import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.security.service.UserDetailsServiceImpl;

@Configuration
public class StompAuthenticationConfig {

    private final UserDetailsServiceImpl userDetailsService;

    public StompAuthenticationConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public ChannelInterceptor channelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // For session-based auth, we can get user from Spring Security context
                    // The user should already be authenticated via HTTP session
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                        // Extract user information from the authenticated user
                        String username = auth.getName();
                        
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                            
                            accessor.setUser(newAuth);
                            
                            // Store user info in session attributes
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes != null) {
                                sessionAttributes.put("username", username);
                                sessionAttributes.put("userId", ((UserDetailsImpl) userDetails).getId());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Authentication failed: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("User not authenticated in HTTP session");
                    }
                }
                
                return message;
            }
        };
    }
}