package com.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageFileDto {

    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String fileType;
    private String iconClass; // CSS class for file type icon
    private String thumbnailUrl; // For images
}