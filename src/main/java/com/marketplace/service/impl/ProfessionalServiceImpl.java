package com.marketplace.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.ProfessionalProfileDto;
import com.marketplace.exception.UserAlreadyExistsException;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.ServiceCategory;
import com.marketplace.model.User;
import com.marketplace.repository.AvailabilityRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.ReviewRepository;
import com.marketplace.repository.ServiceCategoryRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ProfessionalService;
import com.marketplace.util.Constants;

@Service
public class ProfessionalServiceImpl implements ProfessionalService {

    private static final Logger logger = LoggerFactory.getLogger(ProfessionalServiceImpl.class);

    private final ProfessionalProfileRepository profileRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ProfessionalServiceImpl(ProfessionalProfileRepository profileRepository,
                                 ServiceCategoryRepository categoryRepository,
                                 AvailabilityRepository availabilityRepository,
                                 ReviewRepository reviewRepository,
                                 UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.availabilityRepository = availabilityRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ProfessionalProfile createProfile(User user, ProfessionalProfileDto profileDto) {
        logger.info("Creating profile for user ID: {}", user.getId());

        if (profileRepository.existsByUser(user)) {
            throw new UserAlreadyExistsException("Profile already exists for this user");
        }

        ServiceCategory category = categoryRepository.findById(profileDto.getCategoryId())
                .orElseThrow(() -> new UsernameTakenException("Category not found"));

        ProfessionalProfile profile = new ProfessionalProfile();
        profile.setUser(user);
        profile.setBio(profileDto.getBio());
        profile.setCertification(profileDto.getCertification());
        profile.setProfilePhotoUrl(profileDto.getProfilePhotoUrl());
        profile.setCategory(category);
        profile.setHourlyRate(profileDto.getHourlyRate());
        profile.setServiceAreaCity(profileDto.getServiceAreaCity());
        profile.setServiceAreaState(profileDto.getServiceAreaState());

        ProfessionalProfile savedProfile = profileRepository.save(profile);
        logger.info("Profile created successfully with ID: {}", savedProfile.getId());
        return savedProfile;
    }

    @Override
    @Transactional
    public ProfessionalProfile updateProfile(User user, ProfessionalProfileDto profileDto) {
        logger.info("Updating profile for user ID: {}", user.getId());

        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));

        ServiceCategory category = categoryRepository.findById(profileDto.getCategoryId())
                .orElseThrow(() -> new UsernameTakenException("Category not found"));

        profile.setBio(profileDto.getBio());
        profile.setCertification(profileDto.getCertification());
        profile.setProfilePhotoUrl(profileDto.getProfilePhotoUrl());
        profile.setCategory(category);
        profile.setHourlyRate(profileDto.getHourlyRate());
        profile.setServiceAreaCity(profileDto.getServiceAreaCity());
        profile.setServiceAreaState(profileDto.getServiceAreaState());

        ProfessionalProfile updatedProfile = profileRepository.save(profile);
        logger.info("Profile updated successfully with ID: {}", updatedProfile.getId());
        return updatedProfile;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalProfile getProfileByUser(User user) {
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalProfile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProfile(User user) {
        return profileRepository.existsByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public ServiceCategory createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new UserAlreadyExistsException("Category already exists");
        }
        ServiceCategory category = new ServiceCategory(name, description);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void addAvailability(Long profileId, List<Availability> availabilityList) {
        ProfessionalProfile profile = getProfileById(profileId);
        availabilityList.forEach(availability -> availability.setProfessional(profile));
        availabilityRepository.saveAll(availabilityList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Availability> getAvailability(Long profileId) {
        ProfessionalProfile profile = getProfileById(profileId);
        return availabilityRepository.findByProfessionalOrderByDateAscStartTimeAsc(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfessionalProfile> searchProfessionals(String category, String city, Double minRating, Pageable pageable) {
        Long categoryId = null;
        if (category != null && !category.isEmpty()) {
            ServiceCategory cat = categoryRepository.findByName(category).orElse(null);
            if (cat != null) {
                categoryId = cat.getId();
            }
        }

        List<ProfessionalProfile> profiles = profileRepository.findProfessionalsByFilters(
                categoryId, city, minRating);
        
        // For simplicity, we're not implementing full pagination here
        // In a real app, you'd want to do this at the database level
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), profiles.size());
        
        List<ProfessionalProfile> pageContent = profiles.subList(start, end);
        return new PageImpl<>(pageContent, pageable, profiles.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalProfile> getTopRatedProfessionals(int limit) {
        return profileRepository.findAll().stream()
                .sorted((p1, p2) -> Double.compare(p2.getAverageRating(), p1.getAverageRating()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}