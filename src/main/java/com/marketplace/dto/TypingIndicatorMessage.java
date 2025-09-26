package com.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingIndicatorMessage {
    private String username;
    private boolean isTyping;
    private String timestamp;
    
    public TypingIndicatorMessage(String username, boolean isTyping) {
        this.username = username;
        this.isTyping = isTyping;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}