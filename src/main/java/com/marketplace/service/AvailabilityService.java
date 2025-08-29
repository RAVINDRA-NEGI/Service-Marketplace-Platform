package com.marketplace.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.marketplace.dto.AvailabilityDto;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

public interface AvailabilityService {
    
    // Create availability
    Availability createAvailability(User user, AvailabilityDto availabilityDto);
    List<Availability> createBulkAvailability(User user, AvailabilityDto availabilityDto);
    
    // Get availability
    List<Availability> getProfessionalAvailability(User user);
    List<Availability> getAvailabilityByDate(User user, LocalDate date);
    List<Availability> getAvailableSlots(User user, LocalDate startDate, LocalDate endDate);
    
    // Update availability
    Availability updateAvailability(User user, Long availabilityId, AvailabilityDto availabilityDto);
    
    // Delete availability
    void deleteAvailability(User user, Long availabilityId);
    void deleteAvailabilityByDate(User user, LocalDate date);
    
    // Check availability
    boolean isSlotAvailable(ProfessionalProfile professional, LocalDate date, LocalTime startTime, LocalTime endTime);
    boolean hasOverlappingSlots(ProfessionalProfile professional, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    // Get by ID
    Availability getAvailabilityById(Long id);
    Availability getAvailabilityByIdAndProfessional(Long id, User user);
}
