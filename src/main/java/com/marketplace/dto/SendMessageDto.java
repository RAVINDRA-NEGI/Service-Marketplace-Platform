package com.marketplace.dto;

import org.springframework.web.multipart.MultipartFile;

import com.marketplace.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageDto {

    private Long conversationId;
    private String content;
    private MessageType messageType = MessageType.TEXT;
    private MultipartFile file; // For single file upload
    private String fileUrl; // For pre-uploaded files
    private String originalFilename; // For file metadata
}