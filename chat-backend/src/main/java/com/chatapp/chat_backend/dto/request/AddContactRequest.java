package com.chatapp.chat_backend.dto.request;

public record AddContactRequest(
        String email,
        String phone
) {}
