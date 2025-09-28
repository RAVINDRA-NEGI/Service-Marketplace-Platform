package com.marketplace.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.model.MessageFile;
import com.marketplace.model.User;
import com.marketplace.security.service.UserDetailsImpl;
import com.marketplace.service.FileUploadService;
import com.marketplace.service.MessageService;
import com.marketplace.service.UserService;

@Controller
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final MessageService messageService;
    private final UserService userService;

    public FileUploadController(FileUploadService fileUploadService,
                              MessageService messageService,
                              UserService userService) {
        this.fileUploadService = fileUploadService;
        this.messageService = messageService;
        this.userService = userService;
    }

    // Helper method to get current user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userService.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file type and size
            if (!isValidFileType(file.getContentType())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type"));
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest().body(Map.of("error", "File too large"));
            }

            // Upload file and get file info
            MessageFile uploadedFile = fileUploadService.uploadMessageFile(file, conversationId, currentUser);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileId", uploadedFile.getId(),
                "fileUrl", uploadedFile.getFileUrl(),
                "originalFilename", uploadedFile.getOriginalFilename(),
                "fileType", uploadedFile.getFileType(),
                "fileSize", uploadedFile.getFileSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/preview/{fileId}")
    @ResponseBody
    public ResponseEntity<?> getFilePreview(@PathVariable Long fileId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            MessageFile file = fileUploadService.getFileById(fileId);
            
            // Check if user has access to this file (through conversation)
            if (!hasAccessToFile(file, currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            return ResponseEntity.ok(Map.of(
                "fileId", file.getId(),
                "fileUrl", file.getFileUrl(),
                "originalFilename", file.getOriginalFilename(),
                "fileType", file.getFileType(),
                "fileSize", file.getFileSize(),
                "contentType", file.getContentType()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }

        try {
            MessageFile file = fileUploadService.getFileById(fileId);
            
            // Only allow deletion by file owner or conversation participants
            if (!hasAccessToFile(file, currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access denied"));
            }

            fileUploadService.deleteFile(fileId);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods
    private boolean isValidFileType(String contentType) {
        if (contentType == null) return false;
        
        return contentType.startsWith("image/") || 
               contentType.startsWith("application/pdf") ||
               contentType.startsWith("text/") ||
               contentType.startsWith("application/msword") ||
               contentType.startsWith("application/vnd.openxmlformats-officedocument");
    }

    private boolean hasAccessToFile(MessageFile file, User currentUser) {
        // Check if user is part of the conversation that contains this file
        return file.getMessage().getConversation().getClient().getUser().getId().equals(currentUser.getId()) ||
               file.getMessage().getConversation().getProfessional().getUser().getId().equals(currentUser.getId());
    }
}