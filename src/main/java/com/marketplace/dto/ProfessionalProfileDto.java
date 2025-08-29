package com.marketplace.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalProfileDto {
    
    private Long id;
    
    @NotBlank(message = "Bio is required")
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;
    
    @Size(max = 500, message = "Certification cannot exceed 500 characters")
    private String certification;
    
    // File upload fields
    private MultipartFile profilePhoto;
    private String profilePhotoUrl;
    
    private List<MultipartFile> certificates; 
    private List<String> certificateUrls; 
    
    @NotNull(message = "Service category is required")
    private Long categoryId;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be greater than 0")
    private Double hourlyRate;
    
    @NotBlank(message = "Service area city is required")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    private String serviceAreaCity;
    
    @NotBlank(message = "Service area state is required")
    @Size(max = 100, message = "State name cannot exceed 100 characters")  
    private String serviceAreaState;
   
    // Availability fields (for future use)
    private String availabilityDates; 
    private String availabilityTimes;
    
    // Helper method to check if profile photo is uploaded
    public boolean hasProfilePhoto() {
        return profilePhoto != null && !profilePhoto.isEmpty();
    }
    
    // Helper method to check if certificates are uploaded
    public boolean hasCertificates() {
        return certificates != null && !certificates.isEmpty() && 
               certificates.stream().anyMatch(file -> !file.isEmpty());
    }
}