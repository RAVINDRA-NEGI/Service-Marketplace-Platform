package com.marketplace.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "message_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @ToString.Exclude
    private Message message;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_type")
    private String fileType; // photo, document, etc.

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructor
    public MessageFile(Message message, String originalFilename, String storedFilename, 
                      String filePath, String fileUrl, Long fileSize, String contentType) {
        this.message = message;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.fileType = determineFileType(contentType);
    }

    // Helper method to determine file type from content type
    private String determineFileType(String contentType) {
        if (contentType == null) return "unknown";
        
        if (contentType.startsWith("image/")) return "photo";
        if (contentType.startsWith("application/pdf")) return "document";
        if (contentType.startsWith("text/")) return "document";
        if (contentType.startsWith("application/msword") || 
            contentType.startsWith("application/vnd.openxmlformats-officedocument")) return "document";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        
        return "other";
    }
}