package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateContactRequest;
import com.chatapp.chat_backend.dto.response.ContactResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.contact.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public Flux<ContactResponse> getContacts(@AuthenticationPrincipal UserPrincipal principal) {
        return contactService.getContacts(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ContactResponse> addContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateContactRequest request) {
        return contactService.addContact(principal.getId(), request);
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ContactResponse> addContactByPath(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @RequestParam(required = false) String alias) {
        return contactService.addContact(principal.getId(), new CreateContactRequest(userId, alias));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        return contactService.removeContact(principal.getId(), userId);
    }

    @GetMapping("/search")
    public Flux<ContactResponse> searchContacts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query) {
        return contactService.searchContacts(principal.getId(), query);
    }
}
