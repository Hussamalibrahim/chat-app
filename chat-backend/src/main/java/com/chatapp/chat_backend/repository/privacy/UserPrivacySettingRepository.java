package com.chatapp.chat_backend.repository.privacy;

import com.chatapp.chat_backend.model.privacy.UserPrivacySetting;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserPrivacySettingRepository extends ReactiveCrudRepository<UserPrivacySetting, UUID> {

    Mono<UserPrivacySetting> findByUserId(UUID userId);
}

