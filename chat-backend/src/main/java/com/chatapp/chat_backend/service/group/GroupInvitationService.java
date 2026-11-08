package com.chatapp.chat_backend.service.group;

import com.chatapp.chat_backend.dto.event.NotificationEvent;
import com.chatapp.chat_backend.dto.request.InviteGroupMemberRequest;
import com.chatapp.chat_backend.dto.response.ChatMemberResponse;
import com.chatapp.chat_backend.dto.response.GroupInvitationResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.chat.ChatRoomMember;
import com.chatapp.chat_backend.model.chat.GroupRole;
import com.chatapp.chat_backend.model.group.GroupInvitation;
import com.chatapp.chat_backend.model.group.InvitationStatus;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.group.GroupChatRepository;
import com.chatapp.chat_backend.repository.group.GroupInvitationRepository;
import com.chatapp.chat_backend.service.notification.KafkaEventProducer;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupInvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final GroupChatRepository groupChatRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PrivacyService privacyService;
    private final KafkaEventProducer kafkaEventProducer;

    public Mono<GroupInvitationResponse> inviteUser(UUID currentUserId, UUID groupChatId, InviteGroupMemberRequest request) {
        if (currentUserId.equals(request.inviteeId())) {
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR));
        }

        return memberRepository.findByChatRoomIdAndUserId(groupChatId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(inviterMember -> {
                    if (inviterMember.getRole() == GroupRole.MEMBER) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                    }

                    return userRepository.findByIdAndDeletedAtIsNull(request.inviteeId())
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                            .flatMap(invitee -> memberRepository.existsByChatRoomIdAndUserId(groupChatId, request.inviteeId())
                                    .flatMap(alreadyMember -> {
                                        if (alreadyMember) {
                                            return Mono.error(new BusinessException(ErrorCode.ALREADY_A_MEMBER));
                                        }

                                        return privacyService.canUserAddToGroup(currentUserId, request.inviteeId())
                                                .flatMap(allowed -> {
                                                    if (!allowed) {
                                                        return Mono.error(new BusinessException(ErrorCode.PRIVACY_RESTRICTION));
                                                    }

                                                    return invitationRepository.existsByGroupChatIdAndInviteeIdAndStatus(groupChatId, request.inviteeId(), InvitationStatus.PENDING)
                                                            .flatMap(exists -> {
                                                                if (exists) {
                                                                    return Mono.error(new BusinessException(ErrorCode.INVITATION_ALREADY_EXISTS));
                                                                }

                                                                Instant now = Instant.now();
                                                                GroupInvitation invitation = GroupInvitation.builder()
                                                                        .groupChatId(groupChatId)
                                                                        .inviterId(currentUserId)
                                                                        .inviteeId(request.inviteeId())
                                                                        .status(InvitationStatus.PENDING)
                                                                        .createdAt(now)
                                                                        .updatedAt(now)
                                                                        .isNewRecord(true)
                                                                        .build();

                                                                return invitationRepository.save(invitation)
                                                                        .flatMap(saved -> {
                                                                            NotificationEvent event = new NotificationEvent(
                                                                                    UUID.randomUUID(),
                                                                                    request.inviteeId(),
                                                                                    "Group Invitation",
                                                                                    "You have been invited to a group",
                                                                                    groupChatId,
                                                                                    now
                                                                            );
                                                                            return kafkaEventProducer.publishNotification(event).thenReturn(saved);
                                                                        })
                                                                        .flatMap(this::mapToResponse);
                                                            });
                                                });
                                    }));
                });
    }

    public Flux<GroupInvitationResponse> getPendingInvitations(UUID currentUserId) {
        return invitationRepository.findByInviteeIdAndStatus(currentUserId, InvitationStatus.PENDING)
                .flatMap(this::mapToResponse);
    }

    @Transactional
    public Mono<ChatMemberResponse> acceptInvitation(UUID currentUserId, UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVITATION_NOT_FOUND)))
                .flatMap(invitation -> {
                    if (!invitation.getInviteeId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.FORBIDDEN));
                    }
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR));
                    }

                    invitation.setStatus(InvitationStatus.ACCEPTED);
                    invitation.setUpdatedAt(Instant.now());

                    Instant now = Instant.now();
                    ChatRoomMember newMember = ChatRoomMember.builder()
                            .chatRoomId(invitation.getGroupChatId())
                            .userId(currentUserId)
                            .role(GroupRole.MEMBER)
                            .joinedAt(now)
                            .isNewRecord(true)
                            .build();

                    return invitationRepository.save(invitation)
                            .then(memberRepository.save(newMember))
                            .flatMap(saved -> userRepository.findById(currentUserId)
                                    .map(user -> new ChatMemberResponse(user.getId(), user.getUsername(), saved.getRole(), saved.getJoinedAt())));
                });
    }

    public Mono<Void> declineInvitation(UUID currentUserId, UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVITATION_NOT_FOUND)))
                .flatMap(invitation -> {
                    if (!invitation.getInviteeId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.FORBIDDEN));
                    }
                    invitation.setStatus(InvitationStatus.DECLINED);
                    invitation.setUpdatedAt(Instant.now());
                    return invitationRepository.save(invitation).then();
                });
    }

    private Mono<GroupInvitationResponse> mapToResponse(GroupInvitation invitation) {
        return Mono.zip(
                groupChatRepository.findByChatRoomId(invitation.getGroupChatId()).map(g -> g.getName()).defaultIfEmpty("Group"),
                userRepository.findById(invitation.getInviterId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown")
        ).map(tuple -> new GroupInvitationResponse(
                invitation.getId(),
                invitation.getGroupChatId(),
                tuple.getT1(),
                invitation.getInviterId(),
                tuple.getT2(),
                invitation.getInviteeId(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt()
        ));
    }
}

