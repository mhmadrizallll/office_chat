package com.officechat.service;

import com.officechat.dto.CreateMessageRequest;
import com.officechat.dto.MessageResponse;
import com.officechat.entity.Conversation;
import com.officechat.entity.Message;
import com.officechat.entity.User;
import com.officechat.repository.ConversationMemberRepository;
import com.officechat.repository.ConversationRepository;
import com.officechat.repository.MessageRepository;
import com.officechat.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
            Long conversationId,
            Long currentUserId) {

        validateMembership(conversationId, currentUserId);

        return messageRepository
            .findByConversationConversationIdOrderBySentAtAsc(conversationId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            Long currentUserId,
            CreateMessageRequest request) {

        validateMembership(conversationId, currentUserId);

        Conversation conversation = conversationRepository
            .findById(conversationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Percakapan tidak ditemukan"
            ));

        User sender = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pengguna tidak ditemukan"
            ));

        LocalDateTime now = LocalDateTime.now();

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType("TEXT");
        message.setMessageContent(request.messageContent().trim());
        message.setReplyToMessage(null);
        message.setIsDeleted(0);
        message.setSentAt(now);

        Message savedMessage = messageRepository.save(message);

        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        return toResponse(savedMessage);
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
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Kamu bukan anggota percakapan ini"
            );
        }
    }

    private MessageResponse toResponse(Message message) {

        return new MessageResponse(
            message.getMessageId(),
            message.getConversation().getConversationId(),
            message.getSender().getUserId(),
            message.getSender().getFullName(),
            message.getMessageType(),
            message.getMessageContent(),
            message.getSentAt(),
            message.getEditedAt()
        );
    }
}