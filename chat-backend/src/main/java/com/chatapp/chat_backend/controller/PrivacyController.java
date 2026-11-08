package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.UpdatePrivacySettingsRequest;
import com.chatapp.chat_backend.dto.response.BlockedUserResponse;
import com.chatapp.chat_backend.dto.response.PrivacySettingsResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    @GetMapping("/settings")
    public Mono<PrivacySettingsResponse> getPrivacySettings(@AuthenticationPrincipal UserPrincipal principal) {
        return privacyService.getPrivacySettings(principal.getId());
    }

    @PatchMapping("/settings")
    public Mono<PrivacySettingsResponse> updatePrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePrivacySettingsRequest request) {
        return privacyService.updatePrivacySettings(principal.getId(), request);
    }

    @PostMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BlockedUserResponse> blockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        return privacyService.blockUser(principal.getId(), userId);
    }

    @DeleteMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> unblockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        return privacyService.unblockUser(principal.getId(), userId);
    }

    @GetMapping("/blocks")
    public Flux<BlockedUserResponse> getBlockedUsers(@AuthenticationPrincipal UserPrincipal principal) {
        return privacyService.getBlockedUsers(principal.getId());
    }
}
