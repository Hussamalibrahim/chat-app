package com.chatapp.chat_backend.service.message;

import com.chatapp.chat_backend.dto.event.ChatMessageEvent;
import com.chatapp.chat_backend.dto.request.SendMessageRequest;
import com.chatapp.chat_backend.dto.request.UpdateMessageRequest;
import com.chatapp.chat_backend.dto.response.MessageReadResponse;
import com.chatapp.chat_backend.dto.response.MessageResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.chat.ChatRoom;
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
import com.chatapp.chat_backend.service.notification.KafkaEventProducer;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final DirectChatRepository directChatRepository;
    private final UserRepository userRepository;
    private final PrivacyService privacyService;
    private final KafkaEventProducer kafkaEventProducer;

    @Transactional
    public Mono<MessageResponse> sendMessage(UUID currentUserId, UUID chatRoomId, SendMessageRequest request) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(member ->
                        // Check if direct chat and if blocked
                        directChatRepository.findByChatRoomId(chatRoomId)
                                .flatMap(direct -> {
                                    UUID other = direct.getUser1Id().equals(currentUserId) ? direct.getUser2Id() : direct.getUser1Id();
                                    return privacyService.isBlockedBetween(currentUserId, other)
                                            .flatMap(blocked -> blocked
                                                    ? Mono.error(new BusinessException(ErrorCode.USER_BLOCKED))
                                                    : Mono.empty());
                                })
                                .then(Mono.defer(() -> {
                                    Instant now = Instant.now();
                                    Message message = Message.builder()
                                            .chatRoomId(chatRoomId)
                                            .senderId(currentUserId)
                                            .messageType(request.messageType() != null ? request.messageType() : MessageType.TEXT)
                                            .content(request.content().trim())
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .isNewRecord(true)
                                            .build();

                                    return messageRepository.save(message)
                                            .flatMap(saved -> chatRoomRepository.findById(chatRoomId)
                                                    .flatMap(room -> {
                                                        room.setUpdatedAt(now);
                                                        return chatRoomRepository.save(room);
                                                    })
                                                    .thenReturn(saved)
                                            )
                                            .flatMap(savedMsg -> {
                                                ChatMessageEvent event = new ChatMessageEvent(
                                                        UUID.randomUUID(),
                                                        "MESSAGE_CREATED",
                                                        savedMsg.getId(),
                                                        savedMsg.getChatRoomId(),
                                                        savedMsg.getSenderId(),
                                                        savedMsg.getContent(),
                                                        savedMsg.getMessageType(),
                                                        savedMsg.getCreatedAt()
                                                );

                                                return kafkaEventProducer.publishMessageCreated(event)
                                                        .thenReturn(savedMsg);
                                            })
                                            .flatMap(this::mapToResponse);
                                }))
                );
    }

    public Flux<MessageResponse> getMessages(UUID currentUserId, UUID chatRoomId, int page, int size) {
        return memberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .flatMapMany(isMember -> {
                    if (!isMember) {
                        return Flux.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER));
                    }
                    int pageSize = Math.min(Math.max(size, 1), 100);
                    int pageIndex = Math.max(page, 0);
                    return messageRepository.findByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                            chatRoomId,
                            PageRequest.of(pageIndex, pageSize)
                    ).flatMap(this::mapToResponse);
                });
    }

    public Mono<MessageResponse> editMessage(UUID currentUserId, UUID messageId, UpdateMessageRequest request) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_NOT_FOUND)))
                .flatMap(msg -> {
                    if (!msg.getSenderId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.CANNOT_EDIT_OTHERS_MESSAGE));
                    }
                    msg.setContent(request.content().trim());
                    msg.setUpdatedAt(Instant.now());
                    return messageRepository.save(msg).flatMap(this::mapToResponse);
                });
    }

    public Mono<Void> deleteMessage(UUID currentUserId, UUID messageId) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_NOT_FOUND)))
                .flatMap(msg -> {
                    if (msg.getSenderId().equals(currentUserId)) {
                        return performSoftDelete(msg);
                    }
                    return memberRepository.findByChatRoomIdAndUserId(msg.getChatRoomId(), currentUserId)
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                            .flatMap(member -> {
                                if (member.getRole() == GroupRole.MEMBER) {
                                    return Mono.error(new BusinessException(ErrorCode.CANNOT_DELETE_OTHERS_MESSAGE));
                                }
                                return performSoftDelete(msg);
                            });
                });
    }

    public Mono<MessageReadResponse> markAsRead(UUID currentUserId, UUID messageId) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_NOT_FOUND)))
                .flatMap(msg -> memberRepository.existsByChatRoomIdAndUserId(msg.getChatRoomId(), currentUserId)
                        .flatMap(isMember -> {
                            if (!isMember) {
                                return Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER));
                            }
                            return messageReadRepository.findByMessageIdAndUserId(messageId, currentUserId)
                                    .map(existing -> new MessageReadResponse(existing.getId(), existing.getMessageId(), existing.getUserId(), existing.getReadAt()))
                                    .switchIfEmpty(Mono.defer(() -> {
                                        MessageRead read = MessageRead.builder()
                                                .messageId(messageId)
                                                .userId(currentUserId)
                                                .readAt(Instant.now())
                                                .isNewRecord(true)
                                                .build();

                                        return messageReadRepository.save(read)
                                                .flatMap(saved -> {
                                                    ChatMessageEvent event = new ChatMessageEvent(
                                                            UUID.randomUUID(),
                                                            "MESSAGE_READ",
                                                            saved.getMessageId(),
                                                            msg.getChatRoomId(),
                                                            currentUserId,
                                                            null,
                                                            null,
                                                            saved.getReadAt()
                                                    );
                                                    return kafkaEventProducer.publishMessageRead(event).thenReturn(saved);
                                                })
                                                .map(saved -> new MessageReadResponse(
                                                        saved.getId(),
                                                        saved.getMessageId(),
                                                        saved.getUserId(),
                                                        saved.getReadAt()
                                                ));
                                    }));
                        }));
    }

    private Mono<Void> performSoftDelete(Message msg) {
        return messageRepository.softDeleteById(msg.getId())
                .flatMap(updatedRows -> {
                    ChatMessageEvent event = new ChatMessageEvent(
                            UUID.randomUUID(),
                            "MESSAGE_DELETED",
                            msg.getId(),
                            msg.getChatRoomId(),
                            msg.getSenderId(),
                            null,
                            null,
                            Instant.now()
                    );
                    return kafkaEventProducer.publishMessageDeleted(event);
                })
                .then();
    }

    private Mono<MessageResponse> mapToResponse(Message message) {
        return Mono.zip(
                userRepository.findById(message.getSenderId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown"),
                messageReadRepository.countByMessageId(message.getId())
        ).map(tuple -> new MessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                tuple.getT1(),
                message.getMessageType(),
                message.getContent(),
                tuple.getT2(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getDeletedAt()
        ));
    }
}

