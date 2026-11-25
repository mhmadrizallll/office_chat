package com.officechat.controller;

import com.officechat.dto.ConversationResponse;
import com.officechat.dto.CreatePersonalConversationRequest;
import com.officechat.security.UserPrincipal;
import com.officechat.service.ConversationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationResponse> getMyConversations(
            @AuthenticationPrincipal UserPrincipal principal) {

        return conversationService.getMyConversations(
            principal.getUserId()
        );
    }

    @PostMapping("/personal")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createPersonalConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePersonalConversationRequest request) {

        return conversationService.createPersonalConversation(
            principal.getUserId(),
            request
        );
    }
}