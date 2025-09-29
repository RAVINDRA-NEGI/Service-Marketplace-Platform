package com.marketplace.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.exception.FileUploadException;
import com.marketplace.model.MessageFile;
import com.marketplace.model.User;
import com.marketplace.repository.MessageFileRepository;
import com.marketplace.service.FileUploadService;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    private final MessageFileRepository messageFileRepository;

    public FileUploadServiceImpl(MessageFileRepository messageFileRepository) {
        this.messageFileRepository = messageFileRepository;
    }

    @Override
    public MessageFile uploadMessageFile(MultipartFile file, Long conversationId, User user) throws IOException {
        return uploadMessageFile(file, conversationId, user, "message-files");
    }

    @Override
    public MessageFile uploadMessageFile(MultipartFile file, Long conversationId, User user, String customDirectory) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        if (!isValidFileType(file.getContentType())) {
            throw new FileUploadException("Invalid file type: " + file.getContentType());
        }

        if (!isValidFileSize(file.getSize())) {
            throw new FileUploadException("File too large: " + file.getSize() + " bytes");
        }

        // Create upload directory
        String uploadDir = "uploads/" + customDirectory;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = generateUniqueFilename(originalFilename);
        String fileExtension = getFileExtension(originalFilename);

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create MessageFile entity (assuming you'll link it to a message later)
        String fileUrl = "/uploads/" + customDirectory + "/" + uniqueFilename;

        MessageFile messageFile = new MessageFile(null, originalFilename, uniqueFilename, filePath.toString(), fileUrl, file.getSize(), file.getContentType());

        logger.info("File uploaded successfully: {} (ID: {})", originalFilename, messageFile.getId());
        return messageFile;
    }

    @Override
    public MessageFile getFileById(Long fileId) {
        return messageFileRepository.findById(fileId)
                .orElseThrow(() -> new FileUploadException("File not found with ID: " + fileId));
    }

    @Override
    public List<MessageFile> getFilesByMessageId(Long messageId) {
        return messageFileRepository.findByMessageId(messageId);
    }

    @Override
    public void deleteFile(Long fileId) {
        MessageFile file = getFileById(fileId);
        try {
            // Delete file from filesystem
            Path filePath = Paths.get(file.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // Delete from database
            messageFileRepository.deleteById(fileId);
            logger.info("File deleted successfully: {} (ID: {})", file.getOriginalFilename(), fileId);
        } catch (IOException e) {
            throw new FileUploadException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFilesByMessageId(Long messageId) {
        List<MessageFile> files = getFilesByMessageId(messageId);
        for (MessageFile file : files) {
            deleteFile(file.getId());
        }
    }

    @Override
    public boolean isValidFileType(String contentType) {
        if (contentType == null) return false;

        // Allow images
        if (contentType.startsWith("image/")) return true;

        // Allow documents
        if (contentType.equals("application/pdf")) return true;
        if (contentType.startsWith("text/")) return true;
        if (contentType.equals("application/msword")) return true;
        if (contentType.startsWith("application/vnd.openxmlformats-officedocument")) return true;

        return false;
    }

    @Override
    public boolean isValidFileSize(long fileSize) {
        // 10MB limit
        return fileSize <= 10 * 1024 * 1024;
    }

    @Override
    public String getFileType(String contentType) {
        if (contentType == null) return "unknown";

        if (contentType.startsWith("image/")) return "photo";
        if (contentType.equals("application/pdf")) return "document";
        if (contentType.startsWith("text/")) return "document";
        if (contentType.startsWith("application/msword") || 
            contentType.startsWith("application/vnd.openxmlformats-officedocument")) return "document";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";

        return "other";
    }

    @Override
    public String generateUniqueFilename(String originalFilename) {
        if (originalFilename == null) {
            return UUID.randomUUID().toString() + ".tmp";
        }

        String fileExtension = getFileExtension(originalFilename);
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String uniqueName = UUID.randomUUID().toString();

        return uniqueName + fileExtension;
    }

    @Override
    public String getFileUrl(String storedFilename) {
        return "/uploads/message-files/" + storedFilename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}