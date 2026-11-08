package com.chatapp.chat_backend.config;

public final class KafkaConstants {

    private KafkaConstants() {}

    public static final String TOPIC_MESSAGE_CREATED = "chat.message.created";
    public static final String TOPIC_MESSAGE_DELETED = "chat.message.deleted";
    public static final String TOPIC_MESSAGE_READ = "chat.message.read";
    public static final String TOPIC_NOTIFICATION_REQUESTED = "chat.notification.requested";

    public static final String GROUP_MESSAGE_DELIVERY = "chat-message-delivery-group";
    public static final String GROUP_NOTIFICATION = "chat-notification-group";
    public static final String GROUP_ANALYTICS = "chat-analytics-group";
}

