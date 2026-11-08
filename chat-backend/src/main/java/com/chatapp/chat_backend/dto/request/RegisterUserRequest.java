package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        @Email
        String email,

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
        String phone,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
