package com.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineStatusMessage {
    private String username;
    private boolean online;
    private String timestamp;
    
    public OnlineStatusMessage(String username, boolean online) {
        this.username = username;
        this.online = online;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}