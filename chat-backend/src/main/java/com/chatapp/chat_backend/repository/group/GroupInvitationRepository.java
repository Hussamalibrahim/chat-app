package com.chatapp.chat_backend.repository.group;

import com.chatapp.chat_backend.model.group.GroupInvitation;
import com.chatapp.chat_backend.model.group.InvitationStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GroupInvitationRepository extends ReactiveCrudRepository<GroupInvitation, UUID> {

    Flux<GroupInvitation> findByInviteeIdAndStatus(UUID inviteeId, InvitationStatus status);

    Flux<GroupInvitation> findByGroupChatId(UUID groupChatId);

    Mono<GroupInvitation> findByGroupChatIdAndInviteeIdAndStatus(UUID groupChatId, UUID inviteeId, InvitationStatus status);

    Mono<Boolean> existsByGroupChatIdAndInviteeIdAndStatus(UUID groupChatId, UUID inviteeId, InvitationStatus status);
}

