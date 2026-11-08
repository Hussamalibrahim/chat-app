package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.request.UpdateMessageRequest;
import com.chatapp.chat_backend.dto.response.MessageAttachmentResponse;
import com.chatapp.chat_backend.dto.response.MessageReadResponse;
import com.chatapp.chat_backend.dto.response.MessageResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.message.MessageAttachmentService;
import com.chatapp.chat_backend.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageAttachmentService attachmentService;

    @PostMapping("/api/v1/chats/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(principal.getId(), chatId, request);
    }

    @GetMapping("/api/v1/chats/{chatId}/messages")
    public Flux<MessageResponse> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return messageService.getMessages(principal.getId(), chatId, page, size);
    }

    @PatchMapping("/api/v1/messages/{messageId}")
    public Mono<MessageResponse> editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        return messageService.editMessage(principal.getId(), messageId, request);
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId) {
        return messageService.deleteMessage(principal.getId(), messageId);
    }

    @PostMapping("/api/v1/messages/{messageId}/read")
    public Mono<MessageReadResponse> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId) {
        return messageService.markAsRead(principal.getId(), messageId);
    }

    @PostMapping(value = "/api/v1/messages/{messageId}/attachments", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId,
            @RequestPart("file") Mono<FilePart> filePartMono) {
        return filePartMono
                .flatMap(filePart -> attachmentService.uploadAttachment(principal.getId(), messageId, filePart));
    }

    @GetMapping("/api/v1/messages/{messageId}/attachments")
    public Flux<MessageAttachmentResponse> getAttachments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId) {
        return attachmentService.getAttachments(principal.getId(), messageId);
    }
}
