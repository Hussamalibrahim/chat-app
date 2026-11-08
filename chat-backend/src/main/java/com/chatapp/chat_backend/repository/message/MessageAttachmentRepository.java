package com.chatapp.chat_backend.repository.message;

import com.chatapp.chat_backend.model.message.MessageAttachment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface MessageAttachmentRepository extends ReactiveCrudRepository<MessageAttachment, UUID> {

    Flux<MessageAttachment> findByMessageId(UUID messageId);
}

