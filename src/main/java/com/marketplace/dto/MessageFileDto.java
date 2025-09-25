package com.marketplace.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageFileDto {

    private Long id;
    
    private String fileName;
    
    private String fileUrl;
    
    private Long fileSize;
    
    private String contentType;
    
    // For upload
    private MultipartFile file;
}