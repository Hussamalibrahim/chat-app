package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.event.ChatMessageEvent;
import com.chatapp.chat_backend.dto.event.NotificationEvent;
import com.chatapp.chat_backend.dto.response.DirectChatResponse;
import com.chatapp.chat_backend.dto.response.MessageRequestResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.message.MessageRequest;
import com.chatapp.chat_backend.model.message.RequestStatus;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.DirectChatRepository;
import com.chatapp.chat_backend.repository.message.MessageRequestRepository;
import com.chatapp.chat_backend.service.chat.ChatService;
import com.chatapp.chat_backend.service.message.MessageRequestService;
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

class MessageRequestServiceTest {

    private UUID senderId;
    private UUID receiverId;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
    }

    private PrivacyService fakePrivacyService() {
        return new PrivacyService(null, null, null, null) {
            @Override
            public Mono<Boolean> isBlockedBetween(UUID u1, UUID u2) {
                return Mono.just(false);
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
    void sendRequest_CannotRequestSelf() {
        MessageRequestService service = new MessageRequestService(
                TestStubs.of(MessageRequestRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                fakePrivacyService(),
                null,
                fakeKafkaProducer());

        StepVerifier.create(service.sendRequest(senderId, senderId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_REQUEST_SELF)
                .verify();
    }

    @Test
    void sendRequest_Success() {
        User receiver = User.builder().id(receiverId).username("receiver").build();
        User sender = User.builder().id(senderId).username("sender").build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(receiver))
                .on("findById", args -> {
                    UUID id = (UUID) args[0];
                    if (id.equals(senderId))
                        return Mono.just(sender);
                    return Mono.just(receiver);
                })
                .build();

        DirectChatRepository directRepo = TestStubs.of(DirectChatRepository.class)
                .on("existsBetweenUsers", args -> Mono.just(false))
                .build();

        UUID reqId = UUID.randomUUID();
        MessageRequest saved = MessageRequest.builder()
                .id(reqId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(RequestStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        MessageRequestRepository reqRepo = TestStubs.of(MessageRequestRepository.class)
                .on("existsBySenderIdAndReceiverIdAndStatus", args -> Mono.just(false))
                .on("save", args -> Mono.just(saved))
                .build();

        MessageRequestService service = new MessageRequestService(
                reqRepo,
                userRepo,
                directRepo,
                fakePrivacyService(),
                null,
                fakeKafkaProducer());

        StepVerifier.create(service.sendRequest(senderId, receiverId))
                .assertNext(res -> {
                    assertEquals(reqId, res.id());
                    assertEquals(RequestStatus.PENDING, res.status());
                    assertEquals("sender", res.senderUsername());
                    assertEquals("receiver", res.receiverUsername());
                })
                .verifyComplete();
    }

    @Test
    void declineRequest_Success() {
        UUID reqId = UUID.randomUUID();
        MessageRequest req = MessageRequest.builder()
                .id(reqId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(RequestStatus.PENDING)
                .build();

        MessageRequestRepository reqRepo = TestStubs.of(MessageRequestRepository.class)
                .on("findById", args -> Mono.just(req))
                .on("save", args -> Mono.just(req))
                .build();

        MessageRequestService service = new MessageRequestService(
                reqRepo,
                TestStubs.of(UserRepository.class).build(),
                TestStubs.of(DirectChatRepository.class).build(),
                fakePrivacyService(),
                null,
                fakeKafkaProducer());

        StepVerifier.create(service.declineRequest(receiverId, reqId))
                .verifyComplete();

        assertEquals(RequestStatus.DECLINED, req.getStatus());
    }
}
