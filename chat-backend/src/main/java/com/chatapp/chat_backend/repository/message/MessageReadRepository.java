package com.chatapp.chat_backend.repository.message;

import com.chatapp.chat_backend.model.message.MessageRead;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MessageReadRepository extends ReactiveCrudRepository<MessageRead, UUID> {

    Flux<MessageRead> findByMessageId(UUID messageId);

    Mono<MessageRead> findByMessageIdAndUserId(UUID messageId, UUID userId);

    Mono<Boolean> existsByMessageIdAndUserId(UUID messageId, UUID userId);

    Mono<Long> countByMessageId(UUID messageId);
}

