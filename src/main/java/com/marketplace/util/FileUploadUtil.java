package com.marketplace.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public class FileUploadUtil {

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain",
        "application/msword", "application/vnd.openxmlformats-officedocument",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public static boolean isValidFile(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return ALLOWED_FILE_TYPES.stream()
                .anyMatch(allowedType -> contentType.startsWith(allowedType));
    }

    public static String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public static String saveFile(MultipartFile file, String uploadDir, String filename) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());
        
        return filePath.toString();
    }
}