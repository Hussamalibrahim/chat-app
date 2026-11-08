package com.chatapp.chat_backend.repository.chat;

import com.chatapp.chat_backend.model.chat.ChatRoomMember;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ChatRoomMemberRepository extends ReactiveCrudRepository<ChatRoomMember, UUID> {

    Flux<ChatRoomMember> findByChatRoomId(UUID chatRoomId);

    Flux<ChatRoomMember> findByUserId(UUID userId);

    Mono<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    Mono<Boolean> existsByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    Mono<Long> countByChatRoomId(UUID chatRoomId);

    @Modifying
    @Query("DELETE FROM chat_room_members WHERE chat_room_id = :chatRoomId AND user_id = :userId")
    Mono<Integer> deleteByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
}

