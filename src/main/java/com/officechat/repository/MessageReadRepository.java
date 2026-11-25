package com.officechat.repository;

import com.officechat.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReadRepository
        extends JpaRepository<MessageRead, Long> {
}