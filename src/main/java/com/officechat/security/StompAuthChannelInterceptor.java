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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.LocalDateTime;
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
            StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        // 1. Validasi token ketika CONNECT.
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
                        !session.getExpiresAt().isAfter(LocalDateTime.now())) {
                    throw new MessagingException("Sesi login sudah berakhir");
                }

                UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

                if (!(userDetails instanceof UserPrincipal principal)) {
                    throw new MessagingException("Identitas pengguna tidak valid");
                }

                accessor.setUser(principal);

            } catch (JwtException | IllegalArgumentException ex) {
                throw new MessagingException("Token JWT tidak valid", ex);
            }

            return message;
        }

        // 2. Pastikan hanya anggota percakapan yang boleh berlangganan.
        if (StompCommand.SUBSCRIBE.equals(command)) {

            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();

            if (!(principal instanceof UserPrincipal user)) {
                throw new MessagingException("Pengguna belum terautentikasi");
            }

            Matcher matcher = destination == null
                ? null
                : SUBSCRIBE_PATTERN.matcher(destination);

            if (matcher == null || !matcher.matches()) {
                throw new MessagingException("Tujuan subscription tidak diizinkan");
            }

            Long conversationId = Long.valueOf(matcher.group(1));

            boolean isMember = memberRepository
                .existsByConversationConversationIdAndUserUserId(
                    conversationId,
                    user.getUserId()
                );

            if (!isMember) {
                throw new MessagingException(
                    "Kamu bukan anggota percakapan ini"
                );
            }

            return message;
        }

        // 3. Batasi pengiriman hanya melalui endpoint yang ditentukan.
        if (StompCommand.SEND.equals(command)) {

            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();

            if (!(principal instanceof UserPrincipal)) {
                throw new MessagingException("Pengguna belum terautentikasi");
            }

            Matcher matcher = destination == null
                ? null
                : SEND_PATTERN.matcher(destination);

            if (matcher == null || !matcher.matches()) {
                throw new MessagingException("Tujuan pengiriman tidak diizinkan");
            }

            return message;
        }

        return message;
    }
}