package com.chatapp.chat_backend.service.message;

import com.chatapp.chat_backend.dto.response.MessageAttachmentResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.message.MessageAttachment;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.message.MessageAttachmentRepository;
import com.chatapp.chat_backend.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAttachmentService {

    private final MessageAttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final StorageService storageService;

    public Mono<MessageAttachmentResponse> uploadAttachment(UUID currentUserId, UUID messageId, FilePart filePart) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_NOT_FOUND)))
                .flatMap(msg -> {
                    if (!msg.getSenderId().equals(currentUserId)) {
                        return Mono.error(new BusinessException(ErrorCode.FORBIDDEN));
                    }

                    String contentType = filePart.headers().getContentType() != null
                            ? filePart.headers().getContentType().toString()
                            : "application/octet-stream";

                    long contentLength = filePart.headers().getContentLength() > 0
                            ? filePart.headers().getContentLength()
                            : 0L;

                    return storageService.store(filePart)
                            .flatMap(url -> {
                                MessageAttachment attachment = MessageAttachment.builder()
                                        .messageId(messageId)
                                        .fileName(filePart.filename())
                                        .fileType(contentType)
                                        .fileSize(contentLength)
                                        .fileUrl(url)
                                        .createdAt(Instant.now())
                                        .isNewRecord(true)
                                        .build();

                                return attachmentRepository.save(attachment)
                                        .map(saved -> new MessageAttachmentResponse(
                                                saved.getId(),
                                                saved.getMessageId(),
                                                saved.getFileName(),
                                                saved.getFileType(),
                                                saved.getFileSize(),
                                                saved.getFileUrl(),
                                                saved.getCreatedAt()
                                        ));
                            });
                });
    }

    public Flux<MessageAttachmentResponse> getAttachments(UUID currentUserId, UUID messageId) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MESSAGE_NOT_FOUND)))
                .flatMapMany(msg -> memberRepository.existsByChatRoomIdAndUserId(msg.getChatRoomId(), currentUserId)
                        .flatMapMany(isMember -> {
                            if (!isMember) {
                                return Flux.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER));
                            }
                            return attachmentRepository.findByMessageId(messageId)
                                    .map(att -> new MessageAttachmentResponse(
                                            att.getId(),
                                            att.getMessageId(),
                                            att.getFileName(),
                                            att.getFileType(),
                                            att.getFileSize(),
                                            att.getFileUrl(),
                                            att.getCreatedAt()
                                    ));
                        }));
    }
}

