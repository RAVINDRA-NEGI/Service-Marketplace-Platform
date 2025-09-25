package com.marketplace.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.marketplace.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private Long id;
    
    private Long clientId;
    
    private String clientName;
    
    private Long professionalId;
    
    private String professionalName;
    
    private String professionalCategory;
    
    private Long bookingId;
    
    private String lastMessageContent;
    
    private MessageType lastMessageType;
    
    private LocalDateTime lastMessageTime;
    
    private boolean isReadByClient;
    
    private boolean isReadByProfessional;
    
    private int unreadCountClient;
    
    private int unreadCountProfessional;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private boolean isActive = true;
    
    // For new messages
    private MessageDto latestMessage;
    
    // For message history
    private List<MessageDto> messages;
}