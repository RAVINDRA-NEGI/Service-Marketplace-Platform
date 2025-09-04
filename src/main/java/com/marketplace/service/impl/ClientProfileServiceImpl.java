package com.marketplace.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.marketplace.dto.ClientProfileDto;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.User;
import com.marketplace.repository.ClientProfileRepository;
import com.marketplace.service.ClientProfileService;

@Service
public class ClientProfileServiceImpl implements ClientProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ClientProfileServiceImpl.class);

    private final ClientProfileRepository profileRepository;

    public ClientProfileServiceImpl(ClientProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public ClientProfile createProfile(User user, ClientProfileDto profileDto) {
        logger.info("Creating client profile for user ID: {}", user.getId());

        if (profileRepository.existsByUser(user)) {
            throw new UsernameTakenException("Profile already exists for this user");
        }

        ClientProfile profile = new ClientProfile(user);
        updateProfileFields(profile, profileDto);
        
        profile.setProfileComplete(isProfileComplete(profile));
        
        ClientProfile savedProfile = profileRepository.save(profile);
        logger.info("Client profile created successfully with ID: {}", savedProfile.getId());
        return savedProfile;
    }

    @Override
    @Transactional
    public ClientProfile updateProfile(User user, ClientProfileDto profileDto) {
        logger.info("Updating client profile for user ID: {}", user.getId());

        ClientProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException("Profile not found"));

        updateProfileFields(profile, profileDto);
        
        profile.setProfileComplete(isProfileComplete(profile));
        profile.setUpdatedAt(java.time.LocalDateTime.now());
        
        ClientProfile updatedProfile = profileRepository.save(profile);
        logger.info("Client profile updated successfully with ID: {}", updatedProfile.getId());
        return updatedProfile;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientProfile getProfileByUser(User user) {
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException("Profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProfile(User user) {
        return profileRepository.existsByUser(user);
    }

    @Override
    @Transactional
    public ClientProfile updateProfilePhoto(User user, MultipartFile photoFile) throws IOException {
        if (photoFile.isEmpty()) {
            throw new IllegalArgumentException("Photo file is empty");
        }

        
        String contentType = photoFile.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Only JPEG and PNG files are allowed");
        }

       
        Path uploadDir = Paths.get("uploads/client-photos");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        
        String originalFilename = photoFile.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
       
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(photoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        
        ClientProfile profile = getProfileByUser(user);
        profile.setProfilePhotoPath(filePath.toString());
        profile.setProfilePhotoUrl("/uploads/client-photos/" + uniqueFilename);
        
        return profileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProfileComplete(ClientProfile profile) {
        return profile.getFullName() != null && !profile.getFullName().trim().isEmpty() &&
               profile.getEmail() != null && !profile.getEmail().trim().isEmpty() &&
               profile.getCity() != null && !profile.getCity().trim().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientProfile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new UsernameTakenException("Profile not found"));
    }

    // Private helper method to update profile fields
    private void updateProfileFields(ClientProfile profile, ClientProfileDto dto) {
        profile.setFullName(dto.getFullName());
        profile.setPhoneNumber(dto.getPhoneNumber());
        profile.setEmail(dto.getEmail());
        profile.setAddress(dto.getAddress());
        profile.setCity(dto.getCity());
        profile.setState(dto.getState());
        profile.setBio(dto.getBio());
        
        // Only update photo URL if provided in DTO
        if (dto.getProfilePhotoUrl() != null) {
            profile.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        }
    }
}