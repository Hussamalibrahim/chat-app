package com.chatapp.chat_backend.repository.chat;

import com.chatapp.chat_backend.model.chat.DirectChat;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DirectChatRepository extends ReactiveCrudRepository<DirectChat, UUID> {

    Mono<DirectChat> findByChatRoomId(UUID chatRoomId);

    @Query("""
        SELECT * FROM direct_chats
        WHERE (user1_id = :u1 AND user2_id = :u2)
           OR (user1_id = :u2 AND user2_id = :u1)
        LIMIT 1
    """)
    Mono<DirectChat> findBetweenUsers(UUID u1, UUID u2);

    @Query("""
        SELECT COUNT(*) > 0 FROM direct_chats
        WHERE (user1_id = :u1 AND user2_id = :u2)
           OR (user1_id = :u2 AND user2_id = :u1)
    """)
    Mono<Boolean> existsBetweenUsers(UUID u1, UUID u2);
}

