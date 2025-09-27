package com.marketplace.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.marketplace.model.MessageFile;
import com.marketplace.model.User;

public interface FileService {
    
    // File upload and processing
    MessageFile uploadFile(MultipartFile file, Long messageId) throws IOException;
    MessageFile saveFileRecord(String originalFilename, String storedFilename, 
                              String filePath, String fileUrl, Long fileSize, String contentType, Long messageId);
    
    // File retrieval
    MessageFile getFileById(Long fileId);
    MessageFile getFileByStoredName(String storedFilename);
    
    // File validation
    boolean isValidFileType(String contentType);
    boolean isValidFileSize(long fileSize);
    String determineFileType(String contentType);
    
    // File access control
    boolean hasAccessToFile(MessageFile file, User user);
    boolean canDeleteFile(MessageFile file, User user);
    
    // File operations
    void deleteFile(Long fileId);
    void deleteFileFromSystem(String filePath);
    void deleteFilesByMessageId(Long messageId);
    
    // File URL generation
    String generateFileUrl(String storedFilename);
    String generateThumbnailUrl(String storedFilename);
    
    // File path utilities
    String generateStoredFilename(String originalFilename);
    String generateFilePath(String storedFilename);
    
    // File information
    String getFileIconClass(String fileType);
    String getFileSizeString(Long fileSize);
}