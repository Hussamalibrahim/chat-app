package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.AddGroupMemberRequest;
import com.chatapp.chat_backend.dto.request.CreateDirectChatRequest;
import com.chatapp.chat_backend.dto.request.CreateGroupChatRequest;
import com.chatapp.chat_backend.dto.request.UpdateGroupChatRequest;
import com.chatapp.chat_backend.dto.request.UpdateMemberRoleRequest;
import com.chatapp.chat_backend.dto.response.ChatMemberResponse;
import com.chatapp.chat_backend.dto.response.ChatRoomResponse;
import com.chatapp.chat_backend.dto.response.DirectChatResponse;
import com.chatapp.chat_backend.dto.response.GroupChatResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DirectChatResponse> createDirectChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDirectChatRequest request) {
        return chatService.createDirectChat(principal.getId(), request.recipientId());
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<GroupChatResponse> createGroupChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupChatRequest request) {
        return chatService.createGroupChat(principal.getId(), request);
    }

    @PatchMapping("/groups/{groupId}")
    public Mono<GroupChatResponse> updateGroupChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupChatRequest request) {
        return chatService.updateGroupChat(principal.getId(), groupId, request);
    }

    @GetMapping
    public Flux<ChatRoomResponse> getUserChats(@AuthenticationPrincipal UserPrincipal principal) {
        return chatService.getUserChats(principal.getId());
    }

    @GetMapping("/{chatId}")
    public Mono<ChatRoomResponse> getChatDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId) {
        return chatService.getChatDetails(principal.getId(), chatId);
    }

    @GetMapping("/{chatId}/members")
    public Flux<ChatMemberResponse> getChatMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId) {
        return chatService.getChatMembers(principal.getId(), chatId);
    }

    @PostMapping("/{chatId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatMemberResponse> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        return chatService.addMemberToGroup(principal.getId(), chatId, request);
    }

    @PatchMapping("/{chatId}/members/{userId}/role")
    public Mono<ChatMemberResponse> updateMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return chatService.updateMemberRole(principal.getId(), chatId, userId, request);
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId,
            @PathVariable UUID userId) {
        return chatService.removeMember(principal.getId(), chatId, userId);
    }

    @PostMapping("/{chatId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId) {
        return chatService.leaveGroup(principal.getId(), chatId);
    }
}
