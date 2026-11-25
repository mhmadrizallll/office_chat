package com.officechat.service;

import com.officechat.dto.ConversationResponse;
import com.officechat.dto.CreateGroupRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public GroupService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse createGroup(
            Long currentUserId,
            CreateGroupRequest request) {

        User creator = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pengguna pembuat grup tidak ditemukan"
            ));

        if (!"ACTIVE".equals(creator.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Akun pembuat grup tidak aktif"
            );
        }

        // Menghilangkan ID duplikat dan ID pembuat grup.
        Set<Long> memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.remove(currentUserId);

        if (memberIds.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Grup harus memiliki minimal satu anggota selain pembuat"
            );
        }

        List<User> users = userRepository.findAllById(memberIds);

        if (users.size() != memberIds.size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ada ID anggota yang tidak ditemukan"
            );
        }

        for (User user : users) {
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Anggota dengan ID " + user.getUserId() + " tidak aktif"
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // Buat percakapan grup.
        Conversation conversation = new Conversation();
        conversation.setConversationType("GROUP");
        conversation.setConversationName(request.groupName().trim());
        conversation.setCreatedBy(creator);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        Conversation savedConversation =
            conversationRepository.save(conversation);

        // Pembuat grup otomatis menjadi ADMIN.
        List<ConversationMember> memberships = new ArrayList<>();

        ConversationMember creatorMembership = new ConversationMember();
        creatorMembership.setConversation(savedConversation);
        creatorMembership.setUser(creator);
        creatorMembership.setMemberRole("ADMIN");
        creatorMembership.setJoinedAt(now);

        memberships.add(creatorMembership);

        // Anggota lainnya menjadi MEMBER.
        for (User user : users) {

            ConversationMember membership = new ConversationMember();

            membership.setConversation(savedConversation);
            membership.setUser(user);
            membership.setMemberRole("MEMBER");
            membership.setJoinedAt(now);

            memberships.add(membership);
        }

        memberRepository.saveAll(memberships);

        return new ConversationResponse(
            savedConversation.getConversationId(),
            savedConversation.getConversationType(),
            savedConversation.getConversationName(),
            savedConversation.getGroupImage(),
            savedConversation.getCreatedAt()
        );
    }
}