package com.marketplace.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.AvailabilityDto;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.repository.AvailabilityRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.service.AvailabilityService;
import com.marketplace.util.Constants;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    private final AvailabilityRepository availabilityRepository;
    private final ProfessionalProfileRepository profileRepository;

    public AvailabilityServiceImpl(AvailabilityRepository availabilityRepository,
                                 ProfessionalProfileRepository profileRepository) {
        this.availabilityRepository = availabilityRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public Availability createAvailability(User user, AvailabilityDto availabilityDto) {
        logger.info("Creating availability for user ID: {}", user.getId());

        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));

        LocalDate date = availabilityDto.getDate();
        LocalTime startTime = availabilityDto.getStartTime();
        LocalTime endTime = availabilityDto.getEndTime();

        // Check for overlapping slots
        if (hasOverlappingSlots(profile, date, startTime, endTime)) {
            throw new UsernameTakenException("Time slot overlaps with existing availability");
        }

        Availability availability = new Availability(profile, date, startTime, endTime);
        Availability savedAvailability = availabilityRepository.save(availability);
        
        logger.info("Availability created successfully with ID: {}", savedAvailability.getId());
        return savedAvailability;
    }

    @Override
    @Transactional
    public List<Availability> createBulkAvailability(User user, AvailabilityDto availabilityDto) {
        logger.info("Creating bulk availability for user ID: {}", user.getId());

        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));

        List<Availability> createdAvailabilities = new ArrayList<>();
        
        if (availabilityDto.getDates() != null) {
            for (LocalDate date : availabilityDto.getDates()) {
                // Check for overlapping slots
                if (!hasOverlappingSlots(profile, date, availabilityDto.getBulkStartTime(), availabilityDto.getBulkEndTime())) {
                    Availability availability = new Availability(profile, date, availabilityDto.getBulkStartTime(), availabilityDto.getBulkEndTime());
                    createdAvailabilities.add(availability);
                }
            }
        }
        
        List<Availability> savedAvailabilities = availabilityRepository.saveAll(createdAvailabilities);
        logger.info("Bulk availability created successfully, count: {}", savedAvailabilities.size());
        return savedAvailabilities;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Availability> getProfessionalAvailability(User user) {
        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
        return availabilityRepository.findByProfessionalOrderByDateAscStartTimeAsc(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Availability> getAvailabilityByDate(User user, LocalDate date) {
        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
        return availabilityRepository.findByProfessionalAndDateOrderByStartTime(profile, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Availability> getAvailableSlots(User user, LocalDate startDate, LocalDate endDate) {
        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
        return availabilityRepository.findAvailableSlotsByProfessionalAndDateRange(profile, startDate, endDate);
    }

    @Override
    @Transactional
    public Availability updateAvailability(User user, Long availabilityId, AvailabilityDto availabilityDto) {
        Availability availability = getAvailabilityByIdAndProfessional(availabilityId, user);
        
        availability.setDate(availabilityDto.getDate());
        availability.setStartTime(availabilityDto.getStartTime());
        availability.setEndTime(availabilityDto.getEndTime());
        
        return availabilityRepository.save(availability);
    }

    @Override
    @Transactional
    public void deleteAvailability(User user, Long availabilityId) {
        Availability availability = getAvailabilityByIdAndProfessional(availabilityId, user);
        availabilityRepository.delete(availability);
        logger.info("Availability deleted successfully with ID: {}", availabilityId);
    }

    @Override
    @Transactional
    public void deleteAvailabilityByDate(User user, LocalDate date) {
        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
        List<Availability> availabilities = availabilityRepository.findByProfessionalAndDateAndIsBookedFalse(profile, date);
        availabilityRepository.deleteAll(availabilities);
        logger.info("Deleted {} availabilities for date: {}", availabilities.size(), date);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(ProfessionalProfile professional, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return !availabilityRepository.existsByProfessionalAndDateAndStartTimeAndEndTime(
                professional, date, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOverlappingSlots(ProfessionalProfile professional, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Availability> existingSlots = availabilityRepository.findByProfessionalAndDateAndIsBookedFalse(professional, date);
        for (Availability slot : existingSlots) {
            if (slot.overlapsWith(startTime, endTime)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Availability getAvailabilityById(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> new UsernameTakenException("Availability not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Availability getAvailabilityByIdAndProfessional(Long id, User user) {
        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
        
        Availability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new UsernameTakenException("Availability not found"));
                
        if (!availability.getProfessional().getId().equals(profile.getId())) {
            throw new UsernameTakenException("Access denied");
        }
        
        return availability;
    }
}
