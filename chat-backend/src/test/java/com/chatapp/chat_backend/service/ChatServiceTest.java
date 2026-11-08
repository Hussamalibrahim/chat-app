package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.AddGroupMemberRequest;
import com.chatapp.chat_backend.dto.request.CreateGroupChatRequest;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.Role;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.chat.ChatRoom;
import com.chatapp.chat_backend.model.chat.ChatRoomMember;
import com.chatapp.chat_backend.model.chat.ChatRoomType;
import com.chatapp.chat_backend.model.chat.DirectChat;
import com.chatapp.chat_backend.model.chat.GroupRole;
import com.chatapp.chat_backend.model.group.GroupChat;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.chat.DirectChatRepository;
import com.chatapp.chat_backend.repository.group.GroupChatRepository;
import com.chatapp.chat_backend.service.chat.ChatService;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceTest {

    private UUID user1Id;
    private UUID user2Id;
    private User user2;

    @BeforeEach
    void setUp() {
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user2 = User.builder()
                .id(user2Id)
                .username("bob")
                .role(Role.USER)
                .build();
    }

    private PrivacyService fakePrivacyService(boolean blocked, boolean canMessage) {
        return new PrivacyService(null, null, null, null) {
            @Override
            public Mono<Boolean> isBlockedBetween(UUID u1, UUID u2) {
                return Mono.just(blocked);
            }

            @Override
            public Mono<Boolean> canUserMessage(UUID senderId, UUID receiverId) {
                return Mono.just(canMessage);
            }

            @Override
            public Mono<Boolean> canUserAddToGroup(UUID inviterId, UUID inviteeId) {
                return Mono.just(true);
            }
        };
    }

    @Test
    void createDirectChat_CannotChatWithSelf() {
        ChatService chatService = new ChatService(
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(GroupChatRepository.class).build(),
                TestStubs.of(ChatRoomMemberRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(false, true)
        );

        StepVerifier.create(chatService.createDirectChat(user1Id, user1Id))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_CHAT_WITH_SELF)
                .verify();
    }

    @Test
    void createDirectChat_Blocked_ThrowsException() {
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(user2))
                .build();

        ChatService chatService = new ChatService(
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(GroupChatRepository.class).build(),
                TestStubs.of(ChatRoomMemberRepository.class).build(),
                userRepo,
                fakePrivacyService(true, true)
        );

        StepVerifier.create(chatService.createDirectChat(user1Id, user2Id))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.USER_BLOCKED)
                .verify();
    }

    @Test
    void createDirectChat_Success_NewRoom() {
        UUID roomId = UUID.randomUUID();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(user2))
                .build();

        ChatRoom savedRoom = ChatRoom.builder().id(roomId).type(ChatRoomType.DIRECT).createdAt(Instant.now()).build();
        ChatRoomRepository roomRepo = TestStubs.of(ChatRoomRepository.class)
                .on("save", args -> Mono.just(savedRoom))
                .build();

        DirectChatRepository directRepo = TestStubs.of(DirectChatRepository.class)
                .on("findBetweenUsers", args -> Mono.empty())
                .on("save", args -> Mono.just(DirectChat.builder().chatRoomId(roomId).build()))
                .build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("save", args -> Mono.just(ChatRoomMember.builder().build()))
                .build();

        ChatService chatService = new ChatService(
                roomRepo,
                directRepo,
                TestStubs.of(GroupChatRepository.class).build(),
                memberRepo,
                userRepo,
                fakePrivacyService(false, true)
        );

        StepVerifier.create(chatService.createDirectChat(user1Id, user2Id))
                .assertNext(res -> {
                    assertEquals(roomId, res.chatRoomId());
                    assertEquals("bob", res.otherUsername());
                })
                .verifyComplete();
    }

    @Test
    void createGroupChat_Success() {
        UUID roomId = UUID.randomUUID();
        CreateGroupChatRequest req = new CreateGroupChatRequest("Dev Team", "Developers", List.of());

        ChatRoom savedRoom = ChatRoom.builder().id(roomId).type(ChatRoomType.GROUP).createdAt(Instant.now()).build();
        ChatRoomRepository roomRepo = TestStubs.of(ChatRoomRepository.class)
                .on("save", args -> Mono.just(savedRoom))
                .build();

        GroupChatRepository groupRepo = TestStubs.of(GroupChatRepository.class)
                .on("save", args -> Mono.just(GroupChat.builder().chatRoomId(roomId).name("Dev Team").build()))
                .build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("save", args -> Mono.just(ChatRoomMember.builder().build()))
                .on("countByChatRoomId", args -> Mono.just(1L))
                .build();

        ChatService chatService = new ChatService(
                roomRepo,
                TestStubs.of(DirectChatRepository.class).build(),
                groupRepo,
                memberRepo,
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(false, true)
        );

        StepVerifier.create(chatService.createGroupChat(user1Id, req))
                .assertNext(res -> {
                    assertEquals(roomId, res.chatRoomId());
                    assertEquals("Dev Team", res.name());
                    assertEquals(1L, res.memberCount());
                })
                .verifyComplete();
    }

    @Test
    void addMemberToGroup_InsufficientPermissions_ThrowsException() {
        UUID roomId = UUID.randomUUID();
        ChatRoomMember member = ChatRoomMember.builder().chatRoomId(roomId).userId(user1Id).role(GroupRole.MEMBER).build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("findByChatRoomIdAndUserId", args -> Mono.just(member))
                .build();

        ChatService chatService = new ChatService(
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(GroupChatRepository.class).build(),
                memberRepo,
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(false, true)
        );

        StepVerifier.create(chatService.addMemberToGroup(user1Id, roomId, new AddGroupMemberRequest(user2Id, GroupRole.MEMBER)))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_PERMISSIONS)
                .verify();
    }
}
