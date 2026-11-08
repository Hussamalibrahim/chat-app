package com.chatapp.chat_backend.repository.message;

import com.chatapp.chat_backend.model.message.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface MessageRepository extends ReactiveCrudRepository<Message, UUID> {

    Flux<Message> findByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID chatRoomId, Pageable pageable);

    Mono<Message> findByIdAndDeletedAtIsNull(UUID id);

    Mono<Long> countByChatRoomIdAndDeletedAtIsNull(UUID chatRoomId);

    @Query("SELECT COUNT(*) FROM messages WHERE deleted_at IS NULL")
    Mono<Long> countTotalMessages();

    @Query("SELECT COUNT(*) FROM messages WHERE created_at >= :since AND deleted_at IS NULL")
    Mono<Long> countMessagesSince(Instant since);

    @Modifying
    @Query("UPDATE messages SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    Mono<Integer> softDeleteById(UUID id);
}

