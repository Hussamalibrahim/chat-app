package com.chatapp.chat_backend.service.message;

import com.chatapp.chat_backend.dto.event.NotificationEvent;
import com.chatapp.chat_backend.dto.response.DirectChatResponse;
import com.chatapp.chat_backend.dto.response.MessageRequestResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.message.MessageRequest;
import com.chatapp.chat_backend.model.message.RequestStatus;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.DirectChatRepository;
import com.chatapp.chat_backend.repository.message.MessageRequestRepository;
import com.chatapp.chat_backend.service.chat.ChatService;
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
public class MessageRequestService {

    private final MessageRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final DirectChatRepository directChatRepository;
    private final PrivacyService privacyService;
    private final ChatService chatService;
    private final KafkaEventProducer kafkaEventProducer;

    public Mono<MessageRequestResponse> sendRequest(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return Mono.error(new BusinessException(ErrorCode.CANNOT_REQUEST_SELF));
        }

        return userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(targetUser -> privacyService.isBlockedBetween(currentUserId, targetUserId)
                        .flatMap(blocked -> {
                            if (blocked) {
                                return Mono.error(new BusinessException(ErrorCode.USER_BLOCKED));
                            }

                            return directChatRepository.existsBetweenUsers(currentUserId, targetUserId)
                                    .flatMap(exists -> {
                                        if (exists) {
                                            return Mono.error(new BusinessException(ErrorCode.DIRECT_CHAT_ALREADY_EXISTS));
                                        }

                                        return requestRepository.existsBySenderIdAndReceiverIdAndStatus(currentUserId, targetUserId, RequestStatus.PENDING)
                                                .flatMap(alreadyPending -> {
                                                    if (alreadyPending) {
                                                        return Mono.error(new BusinessException(ErrorCode.MESSAGE_REQUEST_ALREADY_EXISTS));
                                                    }

                                                    Instant now = Instant.now();
                                                    MessageRequest req = MessageRequest.builder()
                                                            .senderId(currentUserId)
                                                            .receiverId(targetUserId)
                                                            .status(RequestStatus.PENDING)
                                                            .createdAt(now)
                                                            .updatedAt(now)
                                                            .isNewRecord(true)
                                                            .build();

                                                    return requestRepository.save(req)
                                                            .flatMap(saved -> {
                                                                NotificationEvent event = new NotificationEvent(
                                                                        UUID.randomUUID(),
                                                                        targetUserId,
                                                                        "New Message Request",
                                                                        "You have received a new message request",
                                                                        null,
                                                                        now
                                                                );
                                                                return kafkaEventProducer.publishNotification(event).thenReturn(saved);
                                                            })
                                                            .flatMap(this::mapToResponse);
                                                });
                                    });
                        }));
    }

    public Flux<MessageRequestResponse> getPendingRequests(UUID currentUserId) {
        return requestRepository.findByReceiverIdAndStatus(currentUserId, RequestStatus.PENDING)
                .flatMap(this::mapToResponse);
    }

    public Flux<MessageRequestResponse> getSentPendingRequests(UUID currentUserId) {
        return requestRepository.findBySenderIdAndStatus(currentUserId, RequestStatus.PENDING)
                .flatMap(this::mapToResponse);
    }

    @Transactional
    public Mono<DirectChatResponse> acceptRequest(UUID currentUserId, UUID requestId) {
        return requestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_REQUEST_NOT_FOUND)))
                .flatMap(req -> {
                    if (!req.getReceiverId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.FORBIDDEN));
                    }
                    if (req.getStatus() != RequestStatus.PENDING) {
                        return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR));
                    }

                    req.setStatus(RequestStatus.ACCEPTED);
                    req.setUpdatedAt(Instant.now());

                    return requestRepository.save(req)
                            .then(chatService.createDirectChat(currentUserId, req.getSenderId()));
                });
    }

    public Mono<Void> declineRequest(UUID currentUserId, UUID requestId) {
        return requestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_REQUEST_NOT_FOUND)))
                .flatMap(req -> {
                    if (!req.getReceiverId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.FORBIDDEN));
                    }
                    req.setStatus(RequestStatus.DECLINED);
                    req.setUpdatedAt(Instant.now());
                    return requestRepository.save(req).then();
                });
    }

    private Mono<MessageRequestResponse> mapToResponse(MessageRequest req) {
        return Mono.zip(
                userRepository.findById(req.getSenderId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown"),
                userRepository.findById(req.getReceiverId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown")
        ).map(tuple -> new MessageRequestResponse(
                req.getId(),
                req.getSenderId(),
                tuple.getT1(),
                req.getReceiverId(),
                tuple.getT2(),
                req.getStatus(),
                req.getCreatedAt(),
                req.getUpdatedAt()
        ));
    }
}

