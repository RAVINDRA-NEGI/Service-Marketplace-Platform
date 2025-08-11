package com.marketplace.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.marketplace.dto.ProfessionalProfileDto;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.ServiceCategory;
import com.marketplace.model.User;

public interface ProfessionalService {
    ProfessionalProfile createProfile(User user, ProfessionalProfileDto profileDto);
    ProfessionalProfile updateProfile(User user, ProfessionalProfileDto profileDto);
    ProfessionalProfile getProfileByUser(User user);
    ProfessionalProfile getProfileById(Long id);
    boolean hasProfile(User user);
    
    // Category management
    List<ServiceCategory> getAllCategories();
    ServiceCategory createCategory(String name, String description);
    
    // Availability management
    void addAvailability(Long profileId, List<Availability> availabilityList);
    List<Availability> getAvailability(Long profileId);
    
    // Search and filter
    Page<ProfessionalProfile> searchProfessionals(String category, String city, Double minRating, Pageable pageable);
    List<ProfessionalProfile> getTopRatedProfessionals(int limit);
}
