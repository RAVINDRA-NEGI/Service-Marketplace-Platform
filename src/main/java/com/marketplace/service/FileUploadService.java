package com.marketplace.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.marketplace.model.MessageFile;
import com.marketplace.model.User;

public interface FileUploadService {
    
    // Upload files
    MessageFile uploadMessageFile(MultipartFile file, Long conversationId, User user) throws IOException;
    MessageFile uploadMessageFile(MultipartFile file, Long conversationId, User user, String customDirectory) throws IOException;
    
    // Get files
    MessageFile getFileById(Long fileId);
    java.util.List<MessageFile> getFilesByMessageId(Long messageId);
    
    // Delete files
    void deleteFile(Long fileId);
    void deleteFilesByMessageId(Long messageId);
    
    // Validate files
    boolean isValidFileType(String contentType);
    boolean isValidFileSize(long fileSize);
    
    // Get file info
    String getFileType(String contentType);
    String generateUniqueFilename(String originalFilename);
    
    // Get file URL
    String getFileUrl(String storedFilename);
}