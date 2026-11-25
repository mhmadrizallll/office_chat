package com.officechat.repository;

import com.officechat.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository
        extends JpaRepository<Attachment, Long> {
}