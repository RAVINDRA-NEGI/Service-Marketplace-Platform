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
public class MessageDto {

    private Long id;
    
    private Long conversationId;
    
    private Long senderId;
    
    private String senderName;
    
    private String content;
    
    private MessageType messageType = MessageType.TEXT;
    
    private List<MessageFileDto> files;
    
    private LocalDateTime createdAt;
    
    private boolean isRead;
}