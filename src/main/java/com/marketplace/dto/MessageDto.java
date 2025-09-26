package com.marketplace.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.marketplace.enums.MessageStatus;
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
    private String senderAvatarUrl;
    private String content;
    private MessageType messageType;
    private MessageStatus messageStatus;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private List<MessageFileDto> files;
    private Boolean isFromCurrentUser;
    private String dateSeparator; // For grouping messages by date
}