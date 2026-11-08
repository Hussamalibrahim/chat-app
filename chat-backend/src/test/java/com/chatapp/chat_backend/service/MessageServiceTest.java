package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.event.ChatMessageEvent;
import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.request.UpdateMessageRequest;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.chat.ChatRoom;
import com.chatapp.chat_backend.model.chat.ChatRoomMember;
import com.chatapp.chat_backend.model.chat.GroupRole;
import com.chatapp.chat_backend.model.message.Message;
import com.chatapp.chat_backend.model.message.MessageRead;
import com.chatapp.chat_backend.model.message.MessageType;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.chat.DirectChatRepository;
import com.chatapp.chat_backend.repository.message.MessageReadRepository;
import com.chatapp.chat_backend.repository.message.MessageRepository;
import com.chatapp.chat_backend.service.message.MessageService;
import com.chatapp.chat_backend.service.notification.KafkaEventProducer;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    private UUID userId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
    }

    private PrivacyService fakePrivacyService() {
        return new PrivacyService(null, null, null, null) {
            @Override
            public Mono<Boolean> isBlockedBetween(UUID u1, UUID u2) {
                return Mono.just(false);
            }
        };
    }

    private KafkaEventProducer fakeKafkaProducer(AtomicBoolean messageCreatedFlag, AtomicBoolean messageReadFlag) {
        return new KafkaEventProducer(null, null, null) {
            @Override
            public Mono<Void> publishMessageCreated(ChatMessageEvent event) {
                if (messageCreatedFlag != null) {
                    messageCreatedFlag.set(true);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> publishMessageRead(ChatMessageEvent event) {
                if (messageReadFlag != null) {
                    messageReadFlag.set(true);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> publishMessageDeleted(ChatMessageEvent event) {
                return Mono.empty();
            }
        };
    }

    @Test
    void sendMessage_NotMember_ThrowsException() {
        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("findByChatRoomIdAndUserId", args -> Mono.empty())
                .build();

        MessageService messageService = new MessageService(
                TestStubs.of(MessageRepository.class).build(),
                TestStubs.of(MessageReadRepository.class).build(),
                TestStubs.of(ChatRoomRepository.class).build(),
                memberRepo,
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(),
                fakeKafkaProducer(null, null)
        );

        StepVerifier.create(messageService.sendMessage(userId, roomId, new SendMessageRequest(MessageType.TEXT, "Hello")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.NOT_A_CHAT_MEMBER)
                .verify();
    }

    @Test
    void sendMessage_Success_PersistsAndPublishesKafkaEvent() {
        ChatRoomMember member = ChatRoomMember.builder().chatRoomId(roomId).userId(userId).role(GroupRole.MEMBER).build();
        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("findByChatRoomIdAndUserId", args -> Mono.just(member))
                .build();

        UUID msgId = UUID.randomUUID();
        Message savedMsg = Message.builder()
                .id(msgId)
                .chatRoomId(roomId)
                .senderId(userId)
                .messageType(MessageType.TEXT)
                .content("Hello World")
                .createdAt(Instant.now())
                .build();

        MessageRepository messageRepo = TestStubs.of(MessageRepository.class)
                .on("save", args -> Mono.just(savedMsg))
                .build();

        ChatRoomRepository roomRepo = TestStubs.of(ChatRoomRepository.class)
                .on("findById", args -> Mono.just(ChatRoom.builder().id(roomId).build()))
                .on("save", args -> Mono.just(ChatRoom.builder().id(roomId).build()))
                .build();

        AtomicBoolean kafkaEventPublished = new AtomicBoolean(false);

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findById", args -> Mono.just(User.builder().id(userId).username("alice").build()))
                .build();

        MessageReadRepository readRepo = TestStubs.of(MessageReadRepository.class)
                .on("countByMessageId", args -> Mono.just(0L))
                .build();

        MessageService messageService = new MessageService(
                messageRepo,
                readRepo,
                roomRepo,
                memberRepo,
                TestStubs.of(DirectChatRepository.class).build(),
                userRepo,
                fakePrivacyService(),
                fakeKafkaProducer(kafkaEventPublished, null)
        );

        StepVerifier.create(messageService.sendMessage(userId, roomId, new SendMessageRequest(MessageType.TEXT, "Hello World")))
                .assertNext(res -> {
                    assertEquals(msgId, res.id());
                    assertEquals("Hello World", res.content());
                    assertEquals("alice", res.senderUsername());
                })
                .verifyComplete();

        assertTrue(kafkaEventPublished.get());
    }

    @Test
    void editMessage_OthersMessage_ThrowsException() {
        UUID msgId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Message msg = Message.builder().id(msgId).senderId(otherUserId).content("Original").build();

        MessageRepository messageRepo = TestStubs.of(MessageRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(msg))
                .build();

        MessageService messageService = new MessageService(
                messageRepo,
                TestStubs.of(MessageReadRepository.class).build(),
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(ChatRoomMemberRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(),
                fakeKafkaProducer(null, null)
        );

        StepVerifier.create(messageService.editMessage(userId, msgId, new UpdateMessageRequest("Edited")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_EDIT_OTHERS_MESSAGE)
                .verify();
    }

    @Test
    void markAsRead_Success() {
        UUID msgId = UUID.randomUUID();
        Message msg = Message.builder().id(msgId).chatRoomId(roomId).senderId(UUID.randomUUID()).build();

        MessageRepository messageRepo = TestStubs.of(MessageRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(msg))
                .build();

        ChatRoomMemberRepository memberRepo = TestStubs.of(ChatRoomMemberRepository.class)
                .on("existsByChatRoomIdAndUserId", args -> Mono.just(true))
                .build();

        MessageRead savedRead = MessageRead.builder().id(UUID.randomUUID()).messageId(msgId).userId(userId).readAt(Instant.now()).build();
        MessageReadRepository readRepo = TestStubs.of(MessageReadRepository.class)
                .on("findByMessageIdAndUserId", args -> Mono.empty())
                .on("save", args -> Mono.just(savedRead))
                .build();

        AtomicBoolean readEventPublished = new AtomicBoolean(false);

        MessageService messageService = new MessageService(
                messageRepo,
                readRepo,
                TestStubs.of(ChatRoomRepository.class).build(),
                memberRepo,
                TestStubs.of(DirectChatRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(),
                fakeKafkaProducer(null, readEventPublished)
        );

        StepVerifier.create(messageService.markAsRead(userId, msgId))
                .assertNext(res -> {
                    assertEquals(msgId, res.messageId());
                    assertEquals(userId, res.userId());
                })
                .verifyComplete();

        assertTrue(readEventPublished.get());
    }
}
