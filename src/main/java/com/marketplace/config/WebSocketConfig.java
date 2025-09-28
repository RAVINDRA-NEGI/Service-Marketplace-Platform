package com.marketplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.marketplace.interceptor.WebSocketUserInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketSecurityConfig securityConfig;
    private final WebSocketUserInterceptor userInterceptor;

    public WebSocketConfig(WebSocketSecurityConfig securityConfig, WebSocketUserInterceptor userInterceptor) {
        this.securityConfig = securityConfig;
        this.userInterceptor = userInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configure message broker
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set application destination prefixes
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints with session-based authentication
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // In production, specify exact origins
                .withSockJS();  // Fallback for older browsers
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(userInterceptor, securityConfig.securityInterceptor());
    }
}