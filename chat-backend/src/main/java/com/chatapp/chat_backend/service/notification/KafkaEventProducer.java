package com.chatapp.chat_backend.service.notification;

import com.chatapp.chat_backend.config.KafkaConstants;
import com.chatapp.chat_backend.dto.event.ChatMessageEvent;
import com.chatapp.chat_backend.dto.event.NotificationEvent;
import com.chatapp.chat_backend.model.outbox.OutboxEvent;
import com.chatapp.chat_backend.repository.outbox.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public Mono<Void> publishMessageCreated(ChatMessageEvent event) {
        return saveOutboxAndPublish(
                "MESSAGE",
                event.messageId().toString(),
                KafkaConstants.TOPIC_MESSAGE_CREATED,
                event.chatRoomId().toString(),
                event);
    }

    public Mono<Void> publishMessageDeleted(ChatMessageEvent event) {
        return saveOutboxAndPublish(
                "MESSAGE",
                event.messageId().toString(),
                KafkaConstants.TOPIC_MESSAGE_DELETED,
                event.chatRoomId().toString(),
                event);
    }

    public Mono<Void> publishMessageRead(ChatMessageEvent event) {
        return saveOutboxAndPublish(
                "MESSAGE_READ",
                event.messageId().toString(),
                KafkaConstants.TOPIC_MESSAGE_READ,
                event.chatRoomId().toString(),
                event);
    }

    public Mono<Void> publishNotification(NotificationEvent event) {
        return saveOutboxAndPublish(
                "NOTIFICATION",
                event.eventId().toString(),
                KafkaConstants.TOPIC_NOTIFICATION_REQUESTED,
                event.recipientId().toString(),
                event);
    }

    private Mono<Void> saveOutboxAndPublish(String aggregateType, String aggregateId, String topic, String key,
            Object payload) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(payload))
                .flatMap(json -> {
                    Instant now = Instant.now();
                    OutboxEvent outbox = OutboxEvent.builder()
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(topic)
                            .payload(json)
                            .status("PENDING")
                            .createdAt(now)
                            .isNewRecord(true)
                            .build();

                    return outboxEventRepository.save(outbox)
                            .flatMap(saved -> Mono.fromFuture(kafkaTemplate.send(topic, key, payload))
                                    .flatMap(result -> outboxEventRepository.updateStatus(saved.getId(), "PUBLISHED",
                                            Instant.now()))
                                    .onErrorResume(err -> {
                                        log.warn(
                                                "Direct Kafka publish failed for outbox id {}: {}. Kept PENDING for outbox worker.",
                                                saved.getId(), err.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .then();
    }
}
