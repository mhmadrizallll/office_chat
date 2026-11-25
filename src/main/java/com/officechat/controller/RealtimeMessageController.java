package com.officechat.controller;

import com.officechat.dto.CreateMessageRequest;
import com.officechat.dto.MessageResponse;
import com.officechat.dto.RealtimeMessageRequest;
import com.officechat.security.UserPrincipal;
import com.officechat.service.MessageService;

import jakarta.validation.Valid;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RealtimeMessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeMessageController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate) {

        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/conversations/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Valid RealtimeMessageRequest request,
            Principal principal) {

        if (!(principal instanceof UserPrincipal user)) {
            throw new IllegalStateException("Pengguna tidak terautentikasi");
        }

        MessageResponse savedMessage = messageService.sendMessage(
            conversationId,
            user.getUserId(),
            new CreateMessageRequest(request.messageContent())
        );

        messagingTemplate.convertAndSend(
            "/topic/conversations/" + conversationId,
            savedMessage
        );
    }
}