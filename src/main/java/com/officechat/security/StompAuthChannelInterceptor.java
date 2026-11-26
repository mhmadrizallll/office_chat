
package com.officechat.security;

import com.officechat.entity.UserSession;
import com.officechat.repository.ConversationMemberRepository;
import com.officechat.repository.UserSessionRepository;
import com.officechat.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserSessionRepository sessionRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserDetailsService userDetailsService;

    private static final Pattern SUBSCRIBE_PATTERN =
        Pattern.compile("^/topic/conversations/(\\d+)$");

    private static final Pattern SEND_PATTERN =
        Pattern.compile("^/app/conversations/(\\d+)/send$");

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            UserSessionRepository sessionRepository,
            ConversationMemberRepository memberRepository,
            UserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
            );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        // 1. Autentikasi JWT ketika CONNECT.
        if (StompCommand.CONNECT.equals(command)) {

            String authorization =
                accessor.getFirstNativeHeader("Authorization");

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {
                throw new MessagingException("Token tidak ditemukan");
            }

            String token = authorization.substring(7);

            try {

                Claims claims = jwtService.extractClaims(token);

                String tokenId = claims.getId();
                String email = claims.get("email", String.class);

                if (tokenId == null || email == null) {
                    throw new MessagingException("Token tidak valid");
                }

                UserSession session = sessionRepository
                    .findByTokenIdAndRevokedAtIsNull(tokenId)
                    .orElseThrow(() ->
                        new MessagingException("Sesi login tidak valid")
                    );

                if (session.getExpiresAt() == null ||
                        !session.getExpiresAt().isAfter(
                            java.time.LocalDateTime.now()
                        )) {

                    throw new MessagingException(
                        "Sesi login sudah berakhir"
                    );
                }

                UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

                if (!(userDetails instanceof UserPrincipal principal)) {
                    throw new MessagingException(
                        "Identitas pengguna tidak valid"
                    );
                }

                // Simpan identitas pengguna ke sesi STOMP.
                accessor.setUser(principal);

            } catch (JwtException |
                     IllegalArgumentException |
                     AuthenticationException ex) {

                throw new MessagingException(
                    "Autentikasi WebSocket gagal",
                    ex
                );
            }

            return message;
        }

        // Pastikan pengguna sudah terautentikasi.
        if (accessor.getUser() == null ||
                !(accessor.getUser() instanceof UserPrincipal user)) {

            throw new MessagingException(
                "Pengguna belum terautentikasi"
            );
        }

        // 2. Validasi subscription.
        if (StompCommand.SUBSCRIBE.equals(command)) {

            String destination = accessor.getDestination();

            Matcher matcher = destination == null
                ? null
                : SUBSCRIBE_PATTERN.matcher(destination);

            if (matcher == null || !matcher.matches()) {
                throw new MessagingException(
                    "Tujuan subscription tidak diizinkan"
                );
            }

            Long conversationId = Long.valueOf(matcher.group(1));

            validateMembership(conversationId, user.getUserId());

            return message;
        }

        // 3. Validasi pengiriman pesan.
        if (StompCommand.SEND.equals(command)) {

            String destination = accessor.getDestination();

            Matcher matcher = destination == null
                ? null
                : SEND_PATTERN.matcher(destination);

            if (matcher == null || !matcher.matches()) {
                throw new MessagingException(
                    "Tujuan pengiriman tidak diizinkan"
                );
            }

            Long conversationId = Long.valueOf(matcher.group(1));

            validateMembership(conversationId, user.getUserId());

            return message;
        }

        return message;
    }

    private void validateMembership(
            Long conversationId,
            Long userId) {

        boolean isMember = memberRepository
            .existsByConversationConversationIdAndUserUserId(
                conversationId,
                userId
            );

        if (!isMember) {
            throw new MessagingException(
                "Kamu bukan anggota percakapan ini"
            );
        }
    }
}
