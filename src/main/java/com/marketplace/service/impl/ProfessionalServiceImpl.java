package com.marketplace.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public ProfessionalProfile createProfile(User user, ProfessionalProfileDto profileDto) throws IOException {
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
        profile.setCategory(category);
        profile.setHourlyRate(profileDto.getHourlyRate());
        profile.setServiceAreaCity(profileDto.getServiceAreaCity());
        profile.setServiceAreaState(profileDto.getServiceAreaState());

        // Handle profile photo upload
        if (profileDto.hasProfilePhoto()) {
            try {
                String[] photoUrls = saveProfilePhoto(profileDto.getProfilePhoto());
                profile.setProfilePhotoPath(photoUrls[0]);
                profile.setProfilePhotoUrl(photoUrls[1]);
            } catch (IOException e) {
                logger.error("Failed to upload profile photo during profile creation", e);
                throw new IOException("Failed to upload profile photo: " + e.getMessage(), e);
            }
        }

        // Handle certificates upload
        if (profileDto.hasCertificates()) {
            try {
                String[] certData = saveCertificates(profileDto.getCertificates());
                profile.setCertificatesPath(certData[0]);
                profile.setCertificatesUrls(certData[1]);
            } catch (IOException e) {
                logger.error("Failed to upload certificates during profile creation", e);
                throw new IOException("Failed to upload certificates: " + e.getMessage(), e);
            }
        }

        ProfessionalProfile savedProfile = profileRepository.save(profile);
        logger.info("Profile created successfully with ID: {}", savedProfile.getId());
        return savedProfile;
    }

    @Override
    @Transactional
    public ProfessionalProfile updateProfile(User user, ProfessionalProfileDto profileDto) throws IOException {
        logger.info("Updating profile for user ID: {}", user.getId());

        ProfessionalProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new UsernameTakenException(Constants.PROFILE_NOT_FOUND));

        ServiceCategory category = categoryRepository.findById(profileDto.getCategoryId())
                .orElseThrow(() -> new UsernameTakenException("Category not found"));

        profile.setBio(profileDto.getBio());
        profile.setCertification(profileDto.getCertification());
        profile.setCategory(category);
        profile.setHourlyRate(profileDto.getHourlyRate());
        profile.setServiceAreaCity(profileDto.getServiceAreaCity());
        profile.setServiceAreaState(profileDto.getServiceAreaState());

        // Handle profile photo upload during update
        if (profileDto.hasProfilePhoto()) {
            try {
                String[] photoUrls = saveProfilePhoto(profileDto.getProfilePhoto());
                profile.setProfilePhotoPath(photoUrls[0]);
                profile.setProfilePhotoUrl(photoUrls[1]);
            } catch (IOException e) {
                logger.error("Failed to upload profile photo during profile update", e);
                throw new IOException("Failed to upload profile photo: " + e.getMessage(), e);
            }
        }

        // Handle certificates upload during update
        if (profileDto.hasCertificates()) {
            try {
                String[] certData = saveCertificates(profileDto.getCertificates());
                // Combine with existing certificates
                List<String> allUrls = new ArrayList<>();
                if (profile.getCertificatesUrls() != null && !profile.getCertificatesUrls().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> existingUrls = mapper.readValue(profile.getCertificatesUrls(), new TypeReference<List<String>>(){});
                        allUrls.addAll(existingUrls);
                    } catch (Exception e) {
                        logger.warn("Failed to parse existing certificate URLs", e);
                    }
                }
                
                ObjectMapper mapper = new ObjectMapper();
                List<String> newUrls = mapper.readValue(certData[1], new TypeReference<List<String>>(){});
                allUrls.addAll(newUrls);
                
                profile.setCertificatesPath(certData[0]);
                profile.setCertificatesUrls(mapper.writeValueAsString(allUrls));
            } catch (IOException e) {
                logger.error("Failed to upload certificates during profile update", e);
                throw new IOException("Failed to upload certificates: " + e.getMessage(), e);
            }
        }

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

    @Override
    @Transactional
    public ProfessionalProfile updateProfilePhoto(User user, MultipartFile photoFile) throws IOException {
        if (photoFile.isEmpty()) {
            throw new IllegalArgumentException("Photo file is empty");
        }

        String[] photoUrls = saveProfilePhoto(photoFile);
        
        ProfessionalProfile profile = getProfileByUser(user);
        profile.setProfilePhotoPath(photoUrls[0]);
        profile.setProfilePhotoUrl(photoUrls[1]);
        
        return profileRepository.save(profile);
    }
    
    @Override
    @Transactional
    public ProfessionalProfile updateCertificates(User user, List<MultipartFile> certificateFiles) throws IOException {
        if (certificateFiles == null || certificateFiles.isEmpty()) {
            throw new IllegalArgumentException("No certificate files provided");
        }

        String[] certData = saveCertificates(certificateFiles);

        ProfessionalProfile profile = getProfileByUser(user);
        
        // Combine with existing certificates
        List<String> allUrls = new ArrayList<>();
        if (profile.getCertificatesUrls() != null && !profile.getCertificatesUrls().isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<String> existingUrls = mapper.readValue(profile.getCertificatesUrls(), new TypeReference<List<String>>(){});
                allUrls.addAll(existingUrls);
            } catch (Exception e) {
                logger.warn("Failed to parse existing certificate URLs", e);
            }
        }
        
        ObjectMapper mapper = new ObjectMapper();
        List<String> newUrls = mapper.readValue(certData[1], new TypeReference<List<String>>(){});
        allUrls.addAll(newUrls);

        profile.setCertificatesPath(certData[0]);
        profile.setCertificatesUrls(mapper.writeValueAsString(allUrls));
        
        return profileRepository.save(profile);
    }

    private String[] saveProfilePhoto(MultipartFile photoFile) throws IOException {
        // Validate file type using interface method
        if (!isValidImageFile(photoFile)) {
            throw new IllegalArgumentException("Only JPEG and PNG files are allowed for profile photos");
        }

        // Validate file size (5MB limit)
        if (photoFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile photo size cannot exceed 5MB");
        }

        // Create upload directory
        Path uploadDir = Paths.get("uploads/profile-photos");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename
        String originalFilename = photoFile.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save file
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(photoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return new String[]{filePath.toString(), "/uploads/profile-photos/" + uniqueFilename};
    }

    private String[] saveCertificates(List<MultipartFile> certificateFiles) throws IOException {
        // Validate file types using interface method
        for (MultipartFile file : certificateFiles) {
            if (file.isEmpty()) continue;
            
            if (!isValidCertificateFile(file)) {
                throw new IllegalArgumentException("Only PDF, JPEG, and PNG files are allowed for certificates");
            }
            
            // Validate file size (10MB limit per file)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Certificate file size cannot exceed 10MB: " + file.getOriginalFilename());
            }
        }

        // Create upload directory
        Path uploadDir = Paths.get("uploads/certificates");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        List<String> newUrls = new ArrayList<>();
        List<String> newPaths = new ArrayList<>();

        for (MultipartFile file : certificateFiles) {
            if (file.isEmpty()) continue;
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            newPaths.add(filePath.toString());
            newUrls.add("/uploads/certificates/" + uniqueFilename);
        }

        ObjectMapper mapper = new ObjectMapper();
        return new String[]{String.join(",", newPaths), mapper.writeValueAsString(newUrls)};
    }
}