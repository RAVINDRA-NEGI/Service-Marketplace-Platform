package com.marketplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;

@Configuration
@EnableWebSocketSecurity
public class StompSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Allow connection to WebSocket endpoint
            .simpDestMatchers("/app/**").authenticated()
            .simpDestMatchers("/topic/**").authenticated()
            .simpDestMatchers("/queue/**").authenticated()
            .simpDestMatchers("/user/**").authenticated()
            .anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        // Disable same origin policy for development
        // WARNING: Only use this in development, not in production
        return true;
    }
}