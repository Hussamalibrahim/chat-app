package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.event.NotificationEvent;
import com.chatapp.chat_backend.dto.request.InviteGroupMemberRequest;
import com.chatapp.chat_backend.dto.response.ChatMemberResponse;
import com.chatapp.chat_backend.dto.response.GroupInvitationResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.chat.ChatRoomMember;
import com.chatapp.chat_backend.model.chat.GroupRole;
import com.chatapp.chat_backend.model.group.GroupChat;
import com.chatapp.chat_backend.model.group.GroupInvitation;
import com.chatapp.chat_backend.model.group.InvitationStatus;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.group.GroupChatRepository;
import com.chatapp.chat_backend.repository.group.GroupInvitationRepository;
import com.chatapp.chat_backend.service.group.GroupInvitationService;
import com.chatapp.chat_backend.service.notification.KafkaEventProducer;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GroupInvitationServiceTest {

    private UUID inviterId;
    private UUID inviteeId;
    private UUID groupChatId;

    @BeforeEach
    void setUp() {
        inviterId = UUID.randomUUID();
        inviteeId = UUID.randomUUID();
        groupChatId = UUID.randomUUID();
    }

    private PrivacyService fakePrivacyService(boolean allowed) {
        return new PrivacyService(null, null, null, null) {
            @Override
            public Mono<Boolean> canUserAddToGroup(UUID inviter, UUID invitee) {
                return Mono.just(allowed);
            }
        };
    }

    private KafkaEventProducer fakeKafkaProducer() {
        return new KafkaEventProducer(null, null, null) {
            @Override
            public Mono<Void> publishNotification(NotificationEvent event) {
                return Mono.empty();
            }
        };
    }

    @Test
    void inviteUser_CannotInviteSelf() {
        GroupInvitationService service = new GroupInvitationService(
                TestStubs.of(GroupInvitationRepository.class).build(),
                TestStubs.of(GroupChatRepository.class).build(),
                TestStubs.of(ChatRoomMemberRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        InviteGroupMemberRequest request = new InviteGroupMemberRequest(inviterId);

        StepVerifier.create(service.inviteUser(inviterId, groupChatId, request))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.VALIDATION_ERROR)
                .verify();
    }

    @Test
    void inviteUser_MemberRoleCannotInvite() {
        ChatRoomMember inviterMember = ChatRoomMember.builder()
                .chatRoomId(groupChatId)
                .userId(inviterId)
                .role(GroupRole.MEMBER)
                .build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("findByChatRoomIdAndUserId", args -> Mono.just(inviterMember))
                .build();

        GroupInvitationService service = new GroupInvitationService(
                TestStubs.of(GroupInvitationRepository.class).build(),
                TestStubs.of(GroupChatRepository.class).build(),
                memberRepo,
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        InviteGroupMemberRequest request = new InviteGroupMemberRequest(inviteeId);

        StepVerifier.create(service.inviteUser(inviterId, groupChatId, request))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_PERMISSIONS)
                .verify();
    }

    @Test
    void inviteUser_Success() {
        ChatRoomMember inviterMember = ChatRoomMember.builder()
                .chatRoomId(groupChatId)
                .userId(inviterId)
                .role(GroupRole.ADMIN)
                .build();

        User inviteeUser = User.builder().id(inviteeId).username("invitee").build();
        User inviterUser = User.builder().id(inviterId).username("inviter").build();
        GroupChat groupChat = GroupChat.builder().chatRoomId(groupChatId).name("Cool Group").build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("findByChatRoomIdAndUserId", args -> Mono.just(inviterMember))
                .on("existsByChatRoomIdAndUserId", args -> Mono.just(false))
                .build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(inviteeUser))
                .on("findById", args -> {
                    UUID id = (UUID) args[0];
                    if (id.equals(inviteeId)) return Mono.just(inviteeUser);
                    return Mono.just(inviterUser);
                })
                .build();

        UUID invId = UUID.randomUUID();
        GroupInvitation saved = GroupInvitation.builder()
                .id(invId)
                .groupChatId(groupChatId)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        GroupInvitationRepository invitationRepo = TestStubs.of(GroupInvitationRepository.class)
                .on("existsByGroupChatIdAndInviteeIdAndStatus", args -> Mono.just(false))
                .on("save", args -> Mono.just(saved))
                .build();

        GroupChatRepository groupChatRepo = TestStubs.of(GroupChatRepository.class)
                .on("findByChatRoomId", args -> Mono.just(groupChat))
                .on("findById", args -> Mono.just(groupChat))
                .build();

        GroupInvitationService service = new GroupInvitationService(
                invitationRepo,
                groupChatRepo,
                memberRepo,
                userRepo,
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        InviteGroupMemberRequest request = new InviteGroupMemberRequest(inviteeId);

        StepVerifier.create(service.inviteUser(inviterId, groupChatId, request))
                .assertNext(res -> {
                    assertNotNull(res);
                    assertEquals(invId, res.id());
                    assertEquals("Cool Group", res.groupName());
                    assertEquals(InvitationStatus.PENDING, res.status());
                })
                .verifyComplete();
    }

    @Test
    void acceptInvitation_Success() {
        UUID invId = UUID.randomUUID();
        GroupInvitation invitation = GroupInvitation.builder()
                .id(invId)
                .groupChatId(groupChatId)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .status(InvitationStatus.PENDING)
                .build();

        GroupInvitationRepository invitationRepo = TestStubs.of(GroupInvitationRepository.class)
                .on("findById", args -> Mono.just(invitation))
                .on("save", args -> Mono.just(args[0]))
                .build();

        ChatRoomMember newMember = ChatRoomMember.builder()
                .chatRoomId(groupChatId)
                .userId(inviteeId)
                .role(GroupRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("save", args -> Mono.just(newMember))
                .build();

        User inviteeUser = User.builder().id(inviteeId).username("invitee").build();
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findById", args -> Mono.just(inviteeUser))
                .build();

        GroupInvitationService service = new GroupInvitationService(
                invitationRepo,
                TestStubs.of(GroupChatRepository.class).build(),
                memberRepo,
                userRepo,
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        StepVerifier.create(service.acceptInvitation(inviteeId, invId))
                .assertNext(res -> {
                    assertEquals(inviteeId, res.userId());
                    assertEquals("invitee", res.username());
                    assertEquals(GroupRole.MEMBER, res.role());
                })
                .verifyComplete();
    }

    @Test
    void acceptInvitation_ForbiddenForDifferentUser() {
        UUID invId = UUID.randomUUID();
        GroupInvitation invitation = GroupInvitation.builder()
                .id(invId)
                .groupChatId(groupChatId)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .status(InvitationStatus.PENDING)
                .build();

        GroupInvitationRepository invitationRepo = TestStubs.of(GroupInvitationRepository.class)
                .on("findById", args -> Mono.just(invitation))
                .build();

        GroupInvitationService service = new GroupInvitationService(
                invitationRepo,
                null,
                null,
                null,
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        UUID anotherUserId = UUID.randomUUID();
        StepVerifier.create(service.acceptInvitation(anotherUserId, invId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.FORBIDDEN)
                .verify();
    }

    @Test
    void declineInvitation_Success() {
        UUID invId = UUID.randomUUID();
        GroupInvitation invitation = GroupInvitation.builder()
                .id(invId)
                .groupChatId(groupChatId)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .status(InvitationStatus.PENDING)
                .build();

        GroupInvitationRepository invitationRepo = TestStubs.of(GroupInvitationRepository.class)
                .on("findById", args -> Mono.just(invitation))
                .on("save", args -> Mono.just(args[0]))
                .build();

        GroupInvitationService service = new GroupInvitationService(
                invitationRepo,
                null,
                null,
                null,
                fakePrivacyService(true),
                fakeKafkaProducer()
        );

        StepVerifier.create(service.declineInvitation(inviteeId, invId))
                .verifyComplete();

        assertEquals(InvitationStatus.DECLINED, invitation.getStatus());
    }
}
