package com.marketplace.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.marketplace.model.User;
import com.marketplace.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

		private final UserRepository userRepo;
		public UserDetailsServiceImpl(UserRepository userRepo) {
				this.userRepo = userRepo;
		}
	@Override
	@Transactional
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user  = userRepo.findByUsername(username)
						.orElseThrow(()-> new UsernameNotFoundException("User not Found with username:" + username));
		return UserDetailsImpl.build(user);
	}

}
