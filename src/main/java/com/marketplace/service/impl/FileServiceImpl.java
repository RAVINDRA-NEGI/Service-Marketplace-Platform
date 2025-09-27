package com.marketplace.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.model.MessageFile;
import com.marketplace.model.User;
import com.marketplace.repository.MessageFileRepository;
import com.marketplace.service.FileService;
import com.marketplace.util.MessagingConstants;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private MessageFileRepository messageFileRepository;

    @Override
    public MessageFile uploadFile(MultipartFile file, Long messageId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (!isValidFileSize(file.getSize())) {
            throw new IOException("File size too large");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isValidFileType(contentType)) {
            throw new IOException("Invalid file type: " + contentType);
        }

        // Create upload directory
        Path uploadDir = Paths.get(MessagingConstants.UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateStoredFilename(originalFilename);
        String filePath = generateFilePath(storedFilename);
        String fileUrl = generateFileUrl(storedFilename);

        // Save file
        Path destinationPath = Paths.get(filePath);
        Files.copy(file.getInputStream(), destinationPath);

        // Create file record
        MessageFile messageFile = saveFileRecord(
            originalFilename, storedFilename, filePath, fileUrl, 
            file.getSize(), contentType, messageId);

        return messageFile;
    }

    @Override
    public MessageFile saveFileRecord(String originalFilename, String storedFilename, 
                                    String filePath, String fileUrl, Long fileSize, 
                                    String contentType, Long messageId) {
        
        MessageFile messageFile = new MessageFile();
        messageFile.setOriginalFilename(originalFilename);
        messageFile.setStoredFilename(storedFilename);
        messageFile.setFilePath(filePath);
        messageFile.setFileUrl(fileUrl);
        messageFile.setFileSize(fileSize);
        messageFile.setContentType(contentType);
        messageFile.setFileType(determineFileType(contentType));
        
        return messageFileRepository.save(messageFile);
    }

    @Override
    public MessageFile getFileById(Long fileId) {
        return messageFileRepository.findById(fileId)
                .orElse(null);
    }

    @Override
    public MessageFile getFileByStoredName(String storedFilename) {
        // This would require a custom repository method
        // For now, return null - you'd need to add this to repository
        return null;
    }

    @Override
    public boolean isValidFileType(String contentType) {
        if (contentType == null) return false;
        
        for (String allowedType : MessagingConstants.ALLOWED_FILE_TYPES) {
            if (contentType.startsWith(allowedType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidFileSize(long fileSize) {
        return fileSize <= MessagingConstants.MAX_FILE_SIZE;
    }

    @Override
    public String determineFileType(String contentType) {
        if (contentType == null) return "unknown";
        
        if (contentType.startsWith("image/")) return "photo";
        if (contentType.startsWith("application/pdf")) return "document";
        if (contentType.startsWith("text/")) return "document";
        if (contentType.startsWith("application/msword") || 
            contentType.startsWith("application/vnd.openxmlformats-officedocument")) return "document";
        if (contentType.startsWith("application/vnd.ms-excel") || 
            contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml")) return "document";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        
        return "other";
    }

    @Override
    public boolean hasAccessToFile(MessageFile file, User user) {
        // In a real implementation, this would check if user has access to the conversation
        // containing the message with this file
        return true; // Simplified for now
    }

    @Override
    public boolean canDeleteFile(MessageFile file, User user) {
        // In a real implementation, this would check if user is the sender of the message
        // containing this file
        return true; // Simplified for now
    }

    @Override
    public void deleteFile(Long fileId) {
        MessageFile file = getFileById(fileId);
        if (file != null) {
            deleteFileFromSystem(file.getFilePath());
            messageFileRepository.deleteById(fileId);
        }
    }

    @Override
    public void deleteFileFromSystem(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log the error but don't throw - file might have been deleted already
        }
    }

    @Override
    public void deleteFilesByMessageId(Long messageId) {
        // This would require a custom repository method
        // For now, we'll just log - you'd need to add this to repository
        System.out.println("Deleting files for message ID: " + messageId);
    }

    @Override
    public String generateFileUrl(String storedFilename) {
        return "/uploads/messages/" + storedFilename;
    }

    @Override
    public String generateThumbnailUrl(String storedFilename) {
        // For now, return the same URL
        // In production, you'd generate actual thumbnails
        return generateFileUrl(storedFilename);
    }

    @Override
    public String generateStoredFilename(String originalFilename) {
        if (originalFilename == null) {
            return UUID.randomUUID().toString() + ".bin";
        }
        
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }
        
        String uniqueName = UUID.randomUUID().toString();
        return uniqueName + fileExtension;
    }

    @Override
    public String generateFilePath(String storedFilename) {
        return MessagingConstants.UPLOAD_DIR + storedFilename;
    }

    @Override
    public String getFileIconClass(String fileType) {
        switch (fileType.toLowerCase()) {
            case "photo":
                return "fas fa-image text-primary";
            case "document":
                return "fas fa-file-pdf text-danger";
            case "video":
                return "fas fa-video text-success";
            case "audio":
                return "fas fa-music text-warning";
            default:
                return "fas fa-file text-muted";
        }
    }

    @Override
    public String getFileSizeString(Long fileSize) {
        if (fileSize == null) return "0 KB";
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
}