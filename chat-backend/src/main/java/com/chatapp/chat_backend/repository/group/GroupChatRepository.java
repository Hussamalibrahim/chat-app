package com.chatapp.chat_backend.repository.group;

import com.chatapp.chat_backend.model.group.GroupChat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GroupChatRepository extends ReactiveCrudRepository<GroupChat, UUID> {

    Mono<GroupChat> findByChatRoomId(UUID chatRoomId);

    Flux<GroupChat> findByNameContainingIgnoreCase(String name);
}

