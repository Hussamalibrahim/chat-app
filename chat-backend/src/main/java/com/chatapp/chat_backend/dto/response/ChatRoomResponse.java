package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.chat.ChatRoom;
import com.chatapp.chat_backend.model.chat.ChatRoomType;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        ChatRoomType type,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChatRoomResponse of(ChatRoom room, String displayName) {
        return new ChatRoomResponse(
                room.getId(),
                room.getType(),
                displayName,
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}

