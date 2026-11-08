package com.chatapp.chat_backend.repository.contact;

import com.chatapp.chat_backend.model.contact.Contact;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ContactRepository extends ReactiveCrudRepository<Contact, UUID> {

    Flux<Contact> findByUserId(UUID userId);

    Mono<Contact> findByUserIdAndContactUserId(UUID userId, UUID contactUserId);

    Mono<Boolean> existsByUserIdAndContactUserId(UUID userId, UUID contactUserId);

    @Modifying
    @Query("DELETE FROM contacts WHERE user_id = :userId AND contact_user_id = :contactUserId")
    Mono<Integer> deleteByUserIdAndContactUserId(UUID userId, UUID contactUserId);
}

