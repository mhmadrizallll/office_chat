package com.officechat.service;

import com.officechat.dto.AuthRequest;
import com.officechat.dto.AuthResponse;
// import com.officechat.dto.UserResponse;
import com.officechat.entity.User;
import com.officechat.entity.UserSession;
import com.officechat.repository.UserRepository;
import com.officechat.repository.UserSessionRepository;
import com.officechat.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService) {

        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse login(
            AuthRequest request,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(request.identifier())
            .or(() -> userRepository.findByEmployeeId(request.identifier()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Employee ID/email atau password salah"
            ));

        if (!"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(
                    request.password(),
                    user.getPasswordHash())) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Employee ID/email atau password salah"
            );
        }

        UserPrincipal principal = new UserPrincipal(user);

        String tokenId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(principal, tokenId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt =
            now.plusSeconds(jwtService.getExpiration() / 1000);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenId(tokenId);
        session.setDeviceInfo(
            httpRequest.getHeader("User-Agent")
        );
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);

        sessionRepository.save(session);

        return new AuthResponse(
            token,
            "Bearer",
            jwtService.getExpiration() / 1000,
            userService.getUserById(user.getUserId())
        );
    }

    @Transactional
    public void logout(String token) {

        try {
            String tokenId = jwtService.extractClaims(token).getId();

            UserSession session = sessionRepository
                .findByTokenIdAndRevokedAtIsNull(tokenId)
                .orElse(null);

            if (session != null) {
                session.setRevokedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }

        } catch (io.jsonwebtoken.JwtException
                | IllegalArgumentException ex) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token tidak valid"
            );
        }
    }
}