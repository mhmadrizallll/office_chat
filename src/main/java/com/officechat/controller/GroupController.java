package com.officechat.controller;

import com.officechat.dto.ConversationResponse;
import com.officechat.dto.CreateGroupRequest;
import com.officechat.security.UserPrincipal;
import com.officechat.service.GroupService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request) {

        return groupService.createGroup(
            principal.getUserId(),
            request
        );
    }
}