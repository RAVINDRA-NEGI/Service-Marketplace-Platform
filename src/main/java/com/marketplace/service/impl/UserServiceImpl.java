package com.marketplace.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.UserRegistrationDto;
import com.marketplace.enums.Role;
import com.marketplace.exception.UserAlreadyExistsException;
import com.marketplace.exception.UsernameTakenException;
import com.marketplace.model.User;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.UserService;
import com.marketplace.util.Constants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
	
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;

	@Override
	public User registerClient(UserRegistrationDto registrationDto) {
			return registerUser(registrationDto, Role.CLIENT);
	}

	@Override
	public User registerProfessional(UserRegistrationDto registrationDto) {
		return registerUser(registrationDto, Role.PROFESSIONAL);
	}

//	@Override
//	public User registerAdmin(UserRegistrationDto registrationDto) {
//		// TODO Auto-generated method stub
//		return null;
//	}
	
	private User registerUser(UserRegistrationDto dto , Role role) {
		logger.info("Attempting to register user with email:{}" , dto.getEmail() );
		if(userRepo.existsByEmail(dto.getEmail())) {
			throw new UserAlreadyExistsException(Constants.USER_ALREADY_EXISTS);
		}
		if(userRepo.existsByUsername(dto.getUsername())) {
			throw new UsernameTakenException(Constants.USERNAME_TAKEN);
		}
		
		User user = new User(
				dto.getFullName(),
				dto.getUsername(),
				dto.getEmail(),
				passwordEncoder.encode(dto.getPassword()),
				role
		
				);
		User savedUser = userRepo.save(user);
		logger.info("User registered successfully with ID : {}" , savedUser.getId());
		return savedUser;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Optional<User> findById(Long id) {
	    return userRepo.findById(id);
	}
}
