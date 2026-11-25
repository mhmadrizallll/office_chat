package com.officechat.service;

import com.officechat.dto.ConversationResponse;
import com.officechat.dto.CreatePersonalConversationRequest;
import com.officechat.entity.Conversation;
import com.officechat.entity.ConversationMember;
import com.officechat.entity.User;
import com.officechat.repository.ConversationMemberRepository;
import com.officechat.repository.ConversationRepository;
import com.officechat.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(Long userId) {

        return memberRepository.findByUserUserId(userId)
            .stream()
            .map(member -> member.getConversation())
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ConversationResponse createPersonalConversation(
            Long currentUserId,
            CreatePersonalConversationRequest request) {

        if (currentUserId.equals(request.targetUserId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tidak dapat membuat percakapan dengan diri sendiri"
            );
        }

        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Pengguna login tidak ditemukan"
            ));

        User targetUser = userRepository.findById(request.targetUserId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Pengguna tujuan tidak ditemukan"
            ));

        if (!"ACTIVE".equals(targetUser.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pengguna tujuan tidak aktif"
            );
        }

        // Cek apakah percakapan personal sudah tersedia.
        List<ConversationMember> currentMemberships =
            memberRepository.findByUserUserId(currentUserId);

        for (ConversationMember membership : currentMemberships) {

            Conversation conversation = membership.getConversation();

            if (!"PERSONAL".equals(conversation.getConversationType())) {
                continue;
            }

            List<ConversationMember> members =
                memberRepository.findByConversationConversationId(
                    conversation.getConversationId()
                );

            boolean hasCurrentUser = members.stream()
                .anyMatch(m -> m.getUser().getUserId().equals(currentUserId));

            boolean hasTargetUser = members.stream()
                .anyMatch(m -> m.getUser().getUserId().equals(request.targetUserId()));

            if (members.size() == 2 && hasCurrentUser && hasTargetUser) {
                return toResponse(conversation);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = new Conversation();
        conversation.setConversationType("PERSONAL");
        conversation.setConversationName(null);
        conversation.setCreatedBy(currentUser);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        conversation = conversationRepository.save(conversation);

        ConversationMember member1 = new ConversationMember();
        member1.setConversation(conversation);
        member1.setUser(currentUser);
        member1.setMemberRole("MEMBER");
        member1.setJoinedAt(now);

        ConversationMember member2 = new ConversationMember();
        member2.setConversation(conversation);
        member2.setUser(targetUser);
        member2.setMemberRole("MEMBER");
        member2.setJoinedAt(now);

        memberRepository.saveAll(List.of(member1, member2));

        return toResponse(conversation);
    }

    private ConversationResponse toResponse(Conversation conversation) {

        return new ConversationResponse(
            conversation.getConversationId(),
            conversation.getConversationType(),
            conversation.getConversationName(),
            conversation.getGroupImage(),
            conversation.getCreatedAt()
        );
    }
}