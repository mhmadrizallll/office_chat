package com.officechat.repository;

import com.officechat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationConversationIdOrderBySentAtAsc(
        Long conversationId
    );
}