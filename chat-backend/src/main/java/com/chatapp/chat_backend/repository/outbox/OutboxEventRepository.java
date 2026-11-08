package com.chatapp.chat_backend.repository.outbox;

import com.chatapp.chat_backend.model.outbox.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {

    Flux<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    @Modifying
    @Query("UPDATE outbox_events SET status = :status, processed_at = :processedAt WHERE id = :id")
    Mono<Integer> updateStatus(UUID id, String status, Instant processedAt);
}

