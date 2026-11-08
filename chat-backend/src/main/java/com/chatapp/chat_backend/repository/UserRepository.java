package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    Mono<Boolean> existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Mono<Boolean> existsByPhoneAndDeletedAtIsNull(String phone);

    Mono<Boolean> existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Mono<User> findByIdAndDeletedAtIsNull(UUID id);

    Mono<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Mono<User> findByPhoneAndDeletedAtIsNull(String phone);

    Mono<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Flux<User> findByUsernameContainingIgnoreCaseAndDeletedAtIsNull(String query);

    Flux<User> findAllBy(Pageable pageable);

    Flux<User> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL")
    Mono<Long> countTotalActiveUsers();

    @Query("SELECT COUNT(*) FROM users WHERE created_at >= :since AND deleted_at IS NULL")
    Mono<Long> countNewUsersSince(Instant since);

    @Query("""
        SELECT COUNT(DISTINCT sender_id) FROM messages
        WHERE created_at >= :since AND deleted_at IS NULL
    """)
    Mono<Long> countActiveMessengersSince(Instant since);
}

