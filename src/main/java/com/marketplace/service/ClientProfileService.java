package com.marketplace.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.marketplace.dto.ClientProfileDto;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.User;

public interface ClientProfileService {
    
  
    ClientProfile createProfile(User user, ClientProfileDto profileDto);
    ClientProfile updateProfile(User user, ClientProfileDto profileDto);
    ClientProfile getProfileByUser(User user);
    boolean hasProfile(User user);
  
    ClientProfile updateProfilePhoto(User user, MultipartFile photoFile) throws IOException;
   
    boolean isProfileComplete(ClientProfile profile);
    
    ClientProfile getProfileById(Long id);
}