package com.chatapp.chat_backend.repository.chat;

import com.chatapp.chat_backend.model.chat.ChatRoom;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ChatRoomRepository extends ReactiveCrudRepository<ChatRoom, UUID> {

    @Query("""
        SELECT cr.* FROM chat_rooms cr
        JOIN chat_room_members crm ON cr.id = crm.chat_room_id
        WHERE crm.user_id = :userId
        ORDER BY cr.updated_at DESC
    """)
    Flux<ChatRoom> findChatRoomsByUserId(UUID userId);

    @Query("SELECT COUNT(*) FROM chat_rooms")
    Mono<Long> countTotalRooms();

    @Query("SELECT COUNT(*) FROM chat_rooms WHERE type = 'GROUP'")
    Mono<Long> countGroupRooms();

    @Query("SELECT COUNT(*) FROM chat_rooms WHERE type = 'DIRECT'")
    Mono<Long> countDirectRooms();
}

