
package com.officechat.security;

import com.officechat.service.JwtService;
import com.officechat.entity.UserSession;
import com.officechat.repository.UserSessionRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionRepository sessionRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            UserSessionRepository sessionRepository) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessionRepository = sessionRepository;
    }

    // Endpoint yang tidak membutuhkan autentikasi JWT.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return (
            "POST".equalsIgnoreCase(request.getMethod())
                && (
                    "/api/auth/login".equals(path)
                    || "/api/users".equals(path)
                )
        ) || (
            "GET".equalsIgnoreCase(request.getMethod())
                && "/api/health".equals(path)
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Jika tidak ada token, lanjutkan ke Spring Security.
        // Endpoint yang membutuhkan login akan ditolak oleh SecurityConfig.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Mengambil token dari Authorization header.
            String token = authHeader.substring(7);

            // Validasi dan membaca claims JWT.
            Claims claims = jwtService.extractClaims(token);

            String tokenId = claims.getId();
            String email = claims.get("email", String.class);

            if (tokenId == null || email == null) {
                sendUnauthorized(
                    response,
                    "Token tidak memiliki informasi yang valid"
                );
                return;
            }

            // Memeriksa session token di Oracle.
            UserSession session = sessionRepository
                .findByTokenIdAndRevokedAtIsNull(tokenId)
                .orElse(null);

            if (session == null
                    || session.getExpiresAt() == null
                    || !session.getExpiresAt().isAfter(LocalDateTime.now())) {

                sendUnauthorized(
                    response,
                    "Session tidak ditemukan atau sudah kedaluwarsa"
                );
                return;
            }

            // Membuat autentikasi jika belum tersedia.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                        .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            }

            // Melanjutkan request setelah autentikasi.
            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();

            sendUnauthorized(
                response,
                "Token JWT tidak valid"
            );

        } catch (AuthenticationException ex) {

            SecurityContextHolder.clearContext();

            sendUnauthorized(
                response,
                "User tidak dapat diautentikasi"
            );
        }
    }

    // Mengirim respons 401.
    private void sendUnauthorized(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
            "{\"status\":401,\"message\":\"" + message + "\"}"
        );
    }
}
