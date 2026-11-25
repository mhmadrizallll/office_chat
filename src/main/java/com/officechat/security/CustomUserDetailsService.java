package com.officechat.security;

import com.officechat.entity.User;
import com.officechat.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(identifier)
            .or(() -> userRepository.findByEmployeeId(identifier))
            .orElseThrow(() ->
                new UsernameNotFoundException("Akun tidak ditemukan")
            );

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("Akun tidak aktif");
        }

        return new UserPrincipal(user);
    }
}