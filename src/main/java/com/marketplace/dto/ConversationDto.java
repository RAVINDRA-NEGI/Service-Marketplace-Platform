package com.marketplace.dto;

import java.time.LocalDateTime;

import com.marketplace.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private Long id;
    private Long clientId;  // Client's user ID
    private String clientName;
    private String clientAvatarUrl;
    private Long clientProfileId;  // ClientProfile entity ID
    private Long professionalId;  // Professional's user ID
    private String professionalName;
    private String professionalAvatarUrl;
    private Long professionalProfileId;  // ProfessionalProfile entity ID
    private Long bookingId;
    private String lastMessageContent;
    private MessageType lastMessageType;
    private LocalDateTime lastMessageSentAt;
    private Integer unreadCount;  // Unread count for the current user
    private Boolean isActive;
    private Boolean isClosed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isOnline; // For real-time status
}