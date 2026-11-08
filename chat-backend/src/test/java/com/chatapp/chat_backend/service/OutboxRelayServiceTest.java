package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.model.outbox.OutboxEvent;
import com.chatapp.chat_backend.repository.outbox.OutboxEventRepository;
import com.chatapp.chat_backend.service.notification.OutboxRelayService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxRelayServiceTest {

    @Test
    void publishEvent_Success() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType("MESSAGE")
                .aggregateId("room-123")
                .eventType("chat.message.created")
                .payload("{\"test\":\"data\"}")
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        AtomicBoolean updated = new AtomicBoolean(false);
        OutboxEventRepository repo = TestStubs.of(OutboxEventRepository.class)
                .on("updateStatus", args -> {
                    if (eventId.equals(args[0]) && "PUBLISHED".equals(args[1])) {
                        updated.set(true);
                    }
                    return Mono.just(1);
                })
                .build();

        org.springframework.kafka.core.ProducerFactory<String, Object> pf = 
                TestStubs.of(org.springframework.kafka.core.ProducerFactory.class)
                .on("transactionCapable", args -> false)
                .build();

        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(pf) {
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
                return CompletableFuture.completedFuture(new SendResult<>(null, null));
            }
        };

        OutboxRelayService relayService = new OutboxRelayService(repo, kafkaTemplate);

        StepVerifier.create(relayService.publishEvent(event))
                .verifyComplete();

        assertTrue(updated.get());
    }
}
