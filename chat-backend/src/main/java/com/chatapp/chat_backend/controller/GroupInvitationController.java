package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.InviteGroupMemberRequest;
import com.chatapp.chat_backend.dto.response.ChatMemberResponse;
import com.chatapp.chat_backend.dto.response.GroupInvitationResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.group.GroupInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupInvitationController {

    private final GroupInvitationService groupInvitationService;

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<GroupInvitationResponse> inviteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteGroupMemberRequest request) {
        return groupInvitationService.inviteUser(principal.getId(), groupId, request);
    }

    @GetMapping("/invitations")
    public Flux<GroupInvitationResponse> getPendingInvitations(@AuthenticationPrincipal UserPrincipal principal) {
        return groupInvitationService.getPendingInvitations(principal.getId());
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public Mono<ChatMemberResponse> acceptInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID invitationId) {
        return groupInvitationService.acceptInvitation(principal.getId(), invitationId);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> declineInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID invitationId) {
        return groupInvitationService.declineInvitation(principal.getId(), invitationId);
    }
}
