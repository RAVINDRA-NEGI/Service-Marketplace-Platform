package com.marketplace.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.dto.ProfessionalProfileDto;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.ServiceCategory;
import com.marketplace.model.User;

public interface ProfessionalService {
    
    /**
     * Creates a new professional profile for the user
     * Handles file uploads for profile photo and certificates
     */
    ProfessionalProfile createProfile(User user, ProfessionalProfileDto profileDto) throws IOException;
    
    /**
     * Updates an existing professional profile
     * Handles file uploads for profile photo and certificates
     */
    ProfessionalProfile updateProfile(User user, ProfessionalProfileDto profileDto) throws IOException;
    
    /**
     * Retrieves professional profile by user
     */
    ProfessionalProfile getProfileByUser(User user);
    
    /**
     * Retrieves professional profile by ID
     */
    ProfessionalProfile getProfileById(Long id);
    
    /**
     * Checks if user has a professional profile
     */
    boolean hasProfile(User user);
    
    /**
     * Updates only the profile photo
     */
    ProfessionalProfile updateProfilePhoto(User user, MultipartFile photoFile) throws IOException;
    
    /**
     * Updates only the certificates
     */
    ProfessionalProfile updateCertificates(User user, List<MultipartFile> certificateFiles) throws IOException;
    
    // Category management
    List<ServiceCategory> getAllCategories();
    ServiceCategory createCategory(String name, String description);
    
    // Availability management
    void addAvailability(Long profileId, List<Availability> availabilityList);
    List<Availability> getAvailability(Long profileId);
    
    // Search and discovery
    Page<ProfessionalProfile> searchProfessionals(String category, String city, Double minRating, Pageable pageable);
    List<ProfessionalProfile> getTopRatedProfessionals(int limit);
    
    // Validation helpers
    default boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }
    
    default boolean isValidCertificateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("application/pdf") || 
            contentType.equals("image/jpeg") || 
            contentType.equals("image/png")
        );
    }
}