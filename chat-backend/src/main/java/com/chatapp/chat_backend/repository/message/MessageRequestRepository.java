package com.chatapp.chat_backend.repository.message;

import com.chatapp.chat_backend.model.message.MessageRequest;
import com.chatapp.chat_backend.model.message.RequestStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MessageRequestRepository extends ReactiveCrudRepository<MessageRequest, UUID> {

    Mono<MessageRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    Flux<MessageRequest> findByReceiverIdAndStatus(UUID receiverId, RequestStatus status);

    Flux<MessageRequest> findBySenderIdAndStatus(UUID senderId, RequestStatus status);

    Mono<Boolean> existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, RequestStatus status);
}

