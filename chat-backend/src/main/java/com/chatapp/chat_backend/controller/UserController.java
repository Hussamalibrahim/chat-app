package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.ChangePasswordRequest;
import com.chatapp.chat_backend.dto.request.RegisterUserRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @PatchMapping("/me/contact")
    public Mono<UserResponse> addContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return userService.addContact(principal.getId(), email, phone);
    }

    @GetMapping("/find")
    public Mono<UserResponse> findUser(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return userService.findUser(id, username, email, phone);
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getUserById(principal.getId());
    }

    @GetMapping("/search")
    public reactor.core.publisher.Flux<UserResponse> searchUsers(@RequestParam String query) {
        return userService.searchUsers(query);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.softDeleteUser(principal.getId());
    }

    @PatchMapping("/me")
    public Mono<UserResponse> updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String username) {
        return userService.updateUser(principal.getId(), username);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(principal.getId(), request);
    }
}
