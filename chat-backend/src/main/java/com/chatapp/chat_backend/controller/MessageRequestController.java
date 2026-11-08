package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateMessageRequestDto;
import com.chatapp.chat_backend.dto.response.DirectChatResponse;
import com.chatapp.chat_backend.dto.response.MessageRequestResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.message.MessageRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/message-requests")
@RequiredArgsConstructor
public class MessageRequestController {

    private final MessageRequestService messageRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageRequestResponse> sendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMessageRequestDto request) {
        return messageRequestService.sendRequest(principal.getId(), request.targetUserId());
    }

    @GetMapping("/pending")
    public Flux<MessageRequestResponse> getPendingRequests(@AuthenticationPrincipal UserPrincipal principal) {
        return messageRequestService.getPendingRequests(principal.getId());
    }

    @PostMapping("/{requestId}/accept")
    public Mono<DirectChatResponse> acceptRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        return messageRequestService.acceptRequest(principal.getId(), requestId);
    }

    @PostMapping("/{requestId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> declineRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        return messageRequestService.declineRequest(principal.getId(), requestId);
    }
}
