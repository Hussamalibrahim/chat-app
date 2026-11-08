package com.chatapp.chat_backend.service.notification;

import com.chatapp.chat_backend.config.KafkaConstants;
import com.chatapp.chat_backend.dto.event.ChatMessageEvent;
import com.chatapp.chat_backend.dto.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaEventConsumer {

    @KafkaListener(
            topics = KafkaConstants.TOPIC_MESSAGE_CREATED,
            groupId = KafkaConstants.GROUP_MESSAGE_DELIVERY,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageCreated(ChatMessageEvent event) {
        log.info("Received Kafka event [MESSAGE_CREATED]: messageId={}, chatRoomId={}, senderId={}",
                event.messageId(), event.chatRoomId(), event.senderId());
        // In real-time architecture: dispatch to WebSocket session or push notification
    }

    @KafkaListener(
            topics = KafkaConstants.TOPIC_MESSAGE_DELETED,
            groupId = KafkaConstants.GROUP_MESSAGE_DELIVERY,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageDeleted(ChatMessageEvent event) {
        log.info("Received Kafka event [MESSAGE_DELETED]: messageId={}, chatRoomId={}",
                event.messageId(), event.chatRoomId());
    }

    @KafkaListener(
            topics = KafkaConstants.TOPIC_MESSAGE_READ,
            groupId = KafkaConstants.GROUP_MESSAGE_DELIVERY,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageRead(ChatMessageEvent event) {
        log.info("Received Kafka event [MESSAGE_READ]: messageId={}, readerId={}",
                event.messageId(), event.senderId());
    }

    @KafkaListener(
            topics = KafkaConstants.TOPIC_NOTIFICATION_REQUESTED,
            groupId = KafkaConstants.GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotification(NotificationEvent event) {
        log.info("Received Kafka event [NOTIFICATION_REQUESTED]: recipientId={}, title='{}'",
                event.recipientId(), event.title());
    }
}

