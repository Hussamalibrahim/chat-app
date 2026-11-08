package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.ChangePasswordRequest;
import com.chatapp.chat_backend.dto.request.RegisterUserRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.Role;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserResponse> register(RegisterUserRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.USERNAME_REQUIRED));
        }

        if (request.password() == null || request.password().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.PASSWORD_REQUIRED));
        }

        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPhone = request.phone() != null && !request.phone().isBlank();

        if (!hasEmail && !hasPhone) {
            return Mono.error(new BusinessException(ErrorCode.EMAIL_OR_PHONE_REQUIRED));
        }

        return checkUsername(request.username())
                .then(checkEmail(request.email()))
                .then(checkPhone(request.phone()))
                .then(Mono.defer(() -> {
                    Instant now = Instant.now();
                    User user = User.builder()
                            .username(request.username().trim())
                            .email(hasEmail ? request.email().trim() : null)
                            .phone(hasPhone ? request.phone().trim() : null)
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .role(Role.USER)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return userRepository.save(user);
                }))
                .map(UserResponse::from);
    }

    @Override
    public Mono<UserResponse> addContact(UUID userId, String email, String phone) {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();

        if (hasEmail == hasPhone) {
            return Mono.error(new BusinessException(ErrorCode.CONTACT_ONE_FIELD_REQUIRED));
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (hasEmail) {
                        return addEmail(user, email.trim());
                    }
                    return addPhone(user, phone.trim());
                })
                .map(UserResponse::from);
    }

    @Override
    public Mono<UserResponse> findUser(UUID id, String username, String email, String phone) {
        int providedFields = 0;
        if (id != null) providedFields++;
        if (username != null && !username.isBlank()) providedFields++;
        if (email != null && !email.isBlank()) providedFields++;
        if (phone != null && !phone.isBlank()) providedFields++;

        if (providedFields != 1) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_SEARCH_CRITERIA));
        }

        if (id != null) {
            return userRepository.findByIdAndDeletedAtIsNull(id).map(UserResponse::from);
        }
        if (username != null && !username.isBlank()) {
            return userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim()).map(UserResponse::from);
        }
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim()).map(UserResponse::from);
        }
        return userRepository.findByPhoneAndDeletedAtIsNull(Objects.requireNonNull(phone).trim()).map(UserResponse::from);
    }

    @Override
    public Mono<UserResponse> updateUser(UUID userId, String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.USERNAME_REQUIRED));
        }

        String trimmedUsername = username.trim();

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user ->
                        userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(trimmedUsername)
                                .flatMap(exists -> {
                                    if (exists && !user.getUsername().equalsIgnoreCase(trimmedUsername)) {
                                        return Mono.error(new BusinessException(ErrorCode.USERNAME_ALREADY_REGISTERED));
                                    }

                                    user.setUsername(trimmedUsername);
                                    user.setUpdatedAt(Instant.now());

                                    return userRepository.save(user);
                                })
                )
                .map(UserResponse::from);
    }

    @Override
    public Mono<Void> changePassword(UUID userId, ChangePasswordRequest request) {
        if (request.currentPassword() == null || request.currentPassword().isBlank()
                || request.newPassword() == null || request.newPassword().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.PASSWORD_REQUIRED));
        }

        if (request.currentPassword().equals(request.newPassword())) {
            return Mono.error(new BusinessException(ErrorCode.PASSWORD_UNCHANGED));
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD));
                    }

                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    user.setUpdatedAt(Instant.now());

                    return userRepository.save(user);
                })
                .then();
    }

    @Override
    public Mono<UserResponse> getUserById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .map(UserResponse::from);
    }

    @Override
    public reactor.core.publisher.Flux<UserResponse> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return reactor.core.publisher.Flux.empty();
        }
        return userRepository.findByUsernameContainingIgnoreCaseAndDeletedAtIsNull(query.trim())
                .map(UserResponse::from);
    }

    @Override
    public Mono<Void> softDeleteUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    user.setDeletedAt(Instant.now());
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                })
                .then();
    }

    private Mono<Void> checkUsername(String username) {
        return userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim())
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.USERNAME_ALREADY_REGISTERED))
                        : Mono.empty());
    }

    private Mono<Void> checkEmail(String email) {
        if (email == null || email.isBlank()) {
            return Mono.empty();
        }
        return userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED))
                        : Mono.empty());
    }

    private Mono<Void> checkPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Mono.empty();
        }
        return userRepository.existsByPhoneAndDeletedAtIsNull(phone.trim())
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED))
                        : Mono.empty());
    }

    private Mono<User> addEmail(User user, String email) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.EMAIL_ALREADY_ADDED));
        }

        return userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED));
                    }
                    user.setEmail(email);
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                });
    }

    private Mono<User> addPhone(User user, String phone) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.PHONE_ALREADY_ADDED));
        }

        return userRepository.existsByPhoneAndDeletedAtIsNull(phone)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED));
                    }
                    user.setPhone(phone);
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                });
    }
}
