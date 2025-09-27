package com.marketplace.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.model.MessageFile;
import com.marketplace.model.User;
import com.marketplace.service.ConversationService;
import com.marketplace.util.MessagingConstants;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private ConversationService conversationService;

    // Upload file for message
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long conversationId,
            @RequestParam Long messageId,
            org.springframework.security.core.Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            if (file.getSize() > MessagingConstants.MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size too large"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !isValidFileType(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_INVALID_FILE_TYPE));
            }

            // Create upload directory
            Path uploadDir = Paths.get(MessagingConstants.UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bin";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            // Create message file record
            MessageFile messageFile = conversationService.createMessageFile(
                messageId, originalFilename, uniqueFilename, filePath.toString(), 
                "/uploads/messages/" + uniqueFilename, file.getSize(), contentType);

            return ResponseEntity.ok(Map.of(
                "message", MessagingConstants.SUCCESS_FILE_UPLOADED,
                "data", messageFile
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", MessagingConstants.ERROR_FILE_UPLOAD_FAILED + ": " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Download file
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            org.springframework.security.core.Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            MessageFile messageFile = conversationService.getMessageFileById(fileId);
            if (messageFile == null) {
                return ResponseEntity.notFound().build();
            }

            // Verify user has access to this file
            if (!conversationService.hasAccessToFile(messageFile, currentUser)) {
                return ResponseEntity.status(403).build();
            }

            Path filePath = Paths.get(messageFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + messageFile.getOriginalFilename() + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get file info
    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFileInfo(
            @PathVariable Long fileId,
            org.springframework.security.core.Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            MessageFile messageFile = conversationService.getMessageFileById(fileId);
            if (messageFile == null) {
                return ResponseEntity.notFound().build();
            }

            if (!conversationService.hasAccessToFile(messageFile, currentUser)) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(messageFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete file (only for message owner)
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            org.springframework.security.core.Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }

            MessageFile messageFile = conversationService.getMessageFileById(fileId);
            if (messageFile == null) {
                return ResponseEntity.notFound().build();
            }

            // Only message sender can delete file
            if (!conversationService.canDeleteFile(messageFile, currentUser)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            // Delete file from filesystem
            Path filePath = Paths.get(messageFile.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete database record
            conversationService.deleteMessageFile(fileId);

            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Validate file type
    private boolean isValidFileType(String contentType) {
        for (String allowedType : MessagingConstants.ALLOWED_FILE_TYPES) {
            if (contentType.startsWith(allowedType)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to get current user
    private User getCurrentUser(org.springframework.security.core.Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}