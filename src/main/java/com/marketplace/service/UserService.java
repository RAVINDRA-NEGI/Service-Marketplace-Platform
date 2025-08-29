package com.marketplace.service;

import java.util.Optional;

import com.marketplace.dto.UserRegistrationDto;
import com.marketplace.model.User;

public interface UserService {
	User registerClient(UserRegistrationDto registrationDto);
	User registerProfessional(UserRegistrationDto registrationDto);
//	User registerAdmin(UserRegistrationDto registrationDto);
	Optional<User> findById(Long id);
}
