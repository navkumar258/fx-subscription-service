package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.repository.FxUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FxUserDetailsService implements UserDetailsService {
  private final FxUserRepository fxUserRepository;

  public FxUserDetailsService(FxUserRepository fxUserRepository) {
    this.fxUserRepository = fxUserRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return fxUserRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
