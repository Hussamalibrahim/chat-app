package com.chatapp.chat_backend.service.notification;

import com.chatapp.chat_backend.model.outbox.OutboxEvent;
import com.chatapp.chat_backend.repository.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${chat.outbox.poll-interval-ms:5000}")
    public void processPendingEvents() {
        outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 50))
                .flatMap(this::publishEvent, 5)
                .subscribe(
                        null,
                        err -> log.error("Error in OutboxRelayService worker: {}", err.getMessage())
                );
    }

    public Mono<Void> publishEvent(OutboxEvent event) {
        return Mono.fromFuture(kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()))
                .flatMap(result -> outboxEventRepository.updateStatus(event.getId(), "PUBLISHED", Instant.now()))
                .doOnSuccess(ignored -> log.debug("Outbox event {} relayed successfully to topic {}", event.getId(), event.getEventType()))
                .onErrorResume(err -> {
                    log.warn("Failed to relay outbox event {}: {}", event.getId(), err.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
