package com.chatapp.chat_backend.security.authentication;

import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String principal) {
        return userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(principal)
                .switchIfEmpty(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(principal))
                .switchIfEmpty(userRepository.findByPhoneAndDeletedAtIsNull(principal))
                .map(this::toUserDetails)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_CREDENTIALS)));
    }

    public Mono<UserDetails> findByUserId(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(this::toUserDetails)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_CREDENTIALS)));
    }

    private UserDetails toUserDetails(User user) {
        return new UserPrincipal(user);
    }
}
