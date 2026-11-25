package com.officechat.controller;

import com.officechat.dto.CreateMessageRequest;
import com.officechat.dto.MessageResponse;
import com.officechat.security.UserPrincipal;
import com.officechat.service.MessageService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return messageService.getMessages(
            conversationId,
            principal.getUserId()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMessageRequest request) {

        return messageService.sendMessage(
            conversationId,
            principal.getUserId(),
            request
        );
    }
}