package com.chatapp.chat_backend.service.chat;

import com.chatapp.chat_backend.dto.request.AddGroupMemberRequest;
import com.chatapp.chat_backend.dto.request.CreateGroupChatRequest;
import com.chatapp.chat_backend.dto.request.UpdateGroupChatRequest;
import com.chatapp.chat_backend.dto.request.UpdateMemberRoleRequest;
import com.chatapp.chat_backend.dto.response.ChatMemberResponse;
import com.chatapp.chat_backend.dto.response.ChatRoomResponse;
import com.chatapp.chat_backend.dto.response.DirectChatResponse;
import com.chatapp.chat_backend.dto.response.GroupChatResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.chat.*;
import com.chatapp.chat_backend.model.group.GroupChat;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomMemberRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.chat.DirectChatRepository;
import com.chatapp.chat_backend.repository.group.GroupChatRepository;
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
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final DirectChatRepository directChatRepository;
    private final GroupChatRepository groupChatRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PrivacyService privacyService;

    @Transactional
    public Mono<DirectChatResponse> createDirectChat(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            return Mono.error(new BusinessException(ErrorCode.CANNOT_CHAT_WITH_SELF));
        }

        return userRepository.findByIdAndDeletedAtIsNull(recipientId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(recipient -> privacyService.isBlockedBetween(currentUserId, recipientId)
                        .flatMap(blocked -> {
                            if (blocked) {
                                return Mono.error(new BusinessException(ErrorCode.USER_BLOCKED));
                            }

                            // Check existing direct chat
                            return directChatRepository.findBetweenUsers(currentUserId, recipientId)
                                    .map(direct -> new DirectChatResponse(
                                            direct.getChatRoomId(),
                                            recipient.getId(),
                                            recipient.getUsername(),
                                            direct.getCreatedAt()
                                    ))
                                    .switchIfEmpty(
                                            privacyService.canUserMessage(currentUserId, recipientId)
                                                    .flatMap(allowed -> {
                                                        if (!allowed) {
                                                            return Mono.error(new BusinessException(ErrorCode.PRIVACY_RESTRICTION));
                                                        }

                                                        Instant now = Instant.now();
                                                        ChatRoom room = ChatRoom.builder()
                                                                .type(ChatRoomType.DIRECT)
                                                                .createdAt(now)
                                                                .updatedAt(now)
                                                                .isNewRecord(true)
                                                                .build();

                                                        return chatRoomRepository.save(room)
                                                                .flatMap(savedRoom -> {
                                                                    // Order IDs to respect unique pair constraint
                                                                    UUID u1 = currentUserId.compareTo(recipientId) < 0 ? currentUserId : recipientId;
                                                                    UUID u2 = currentUserId.compareTo(recipientId) < 0 ? recipientId : currentUserId;

                                                                    DirectChat direct = DirectChat.builder()
                                                                            .chatRoomId(savedRoom.getId())
                                                                            .user1Id(u1)
                                                                            .user2Id(u2)
                                                                            .createdAt(now)
                                                                            .isNewRecord(true)
                                                                            .build();

                                                                    ChatRoomMember m1 = ChatRoomMember.builder()
                                                                            .chatRoomId(savedRoom.getId())
                                                                            .userId(currentUserId)
                                                                            .role(GroupRole.MEMBER)
                                                                            .joinedAt(now)
                                                                            .isNewRecord(true)
                                                                            .build();

                                                                    ChatRoomMember m2 = ChatRoomMember.builder()
                                                                            .chatRoomId(savedRoom.getId())
                                                                            .userId(recipientId)
                                                                            .role(GroupRole.MEMBER)
                                                                            .joinedAt(now)
                                                                            .isNewRecord(true)
                                                                            .build();

                                                                    return directChatRepository.save(direct)
                                                                            .then(memberRepository.save(m1))
                                                                            .then(memberRepository.save(m2))
                                                                            .thenReturn(new DirectChatResponse(
                                                                                    savedRoom.getId(),
                                                                                    recipient.getId(),
                                                                                    recipient.getUsername(),
                                                                                    now
                                                                            ));
                                                                });
                                                    })
                                    );
                        }));
    }

    @Transactional
    public Mono<GroupChatResponse> createGroupChat(UUID currentUserId, CreateGroupChatRequest request) {
        Instant now = Instant.now();
        ChatRoom room = ChatRoom.builder()
                .type(ChatRoomType.GROUP)
                .createdAt(now)
                .updatedAt(now)
                .isNewRecord(true)
                .build();

        return chatRoomRepository.save(room)
                .flatMap(savedRoom -> {
                    GroupChat groupChat = GroupChat.builder()
                            .chatRoomId(savedRoom.getId())
                            .name(request.name().trim())
                            .description(request.description() != null ? request.description().trim() : null)
                            .createdBy(currentUserId)
                            .createdAt(now)
                            .updatedAt(now)
                            .isNewRecord(true)
                            .build();

                    ChatRoomMember owner = ChatRoomMember.builder()
                            .chatRoomId(savedRoom.getId())
                            .userId(currentUserId)
                            .role(GroupRole.OWNER)
                            .joinedAt(now)
                            .isNewRecord(true)
                            .build();

                    return groupChatRepository.save(groupChat)
                            .then(memberRepository.save(owner))
                            .then(Mono.defer(() -> {
                                if (request.memberIds() != null && !request.memberIds().isEmpty()) {
                                    return Flux.fromIterable(request.memberIds())
                                            .filter(id -> !id.equals(currentUserId))
                                            .flatMap(memberId -> privacyService.canUserAddToGroup(currentUserId, memberId)
                                                    .filter(Boolean::booleanValue)
                                                    .flatMap(allowed -> memberRepository.save(ChatRoomMember.builder()
                                                            .chatRoomId(savedRoom.getId())
                                                            .userId(memberId)
                                                            .role(GroupRole.MEMBER)
                                                            .joinedAt(now)
                                                            .isNewRecord(true)
                                                            .build())))
                                            .then();
                                }
                                return Mono.empty();
                            }))
                            .then(memberRepository.countByChatRoomId(savedRoom.getId()))
                            .map(count -> new GroupChatResponse(
                                    savedRoom.getId(),
                                    groupChat.getName(),
                                    groupChat.getDescription(),
                                    groupChat.getCreatedBy(),
                                    count,
                                    groupChat.getCreatedAt(),
                                    groupChat.getUpdatedAt()
                            ));
                });
    }

    public Mono<GroupChatResponse> updateGroupChat(UUID currentUserId, UUID chatRoomId, UpdateGroupChatRequest request) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(member -> {
                    if (member.getRole() == GroupRole.MEMBER) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                    }
                    return groupChatRepository.findByChatRoomId(chatRoomId)
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND)))
                            .flatMap(group -> {
                                group.setName(request.name().trim());
                                if (request.description() != null) {
                                    group.setDescription(request.description().trim());
                                }
                                group.setUpdatedAt(Instant.now());
                                return groupChatRepository.save(group)
                                        .then(memberRepository.countByChatRoomId(chatRoomId))
                                        .map(count -> new GroupChatResponse(
                                                group.getChatRoomId(),
                                                group.getName(),
                                                group.getDescription(),
                                                group.getCreatedBy(),
                                                count,
                                                group.getCreatedAt(),
                                                group.getUpdatedAt()
                                        ));
                            });
                });
    }

    public Flux<ChatRoomResponse> getUserChats(UUID currentUserId) {
        return chatRoomRepository.findChatRoomsByUserId(currentUserId)
                .flatMap(room -> {
                    if (room.getType() == ChatRoomType.GROUP) {
                        return groupChatRepository.findByChatRoomId(room.getId())
                                .map(group -> ChatRoomResponse.of(room, group.getName()))
                                .defaultIfEmpty(ChatRoomResponse.of(room, "Group Chat"));
                    } else {
                        return directChatRepository.findByChatRoomId(room.getId())
                                .flatMap(direct -> {
                                    UUID otherUserId = direct.getUser1Id().equals(currentUserId) ? direct.getUser2Id() : direct.getUser1Id();
                                    return userRepository.findById(otherUserId)
                                            .map(u -> ChatRoomResponse.of(room, u.getUsername()))
                                            .defaultIfEmpty(ChatRoomResponse.of(room, "Direct Chat"));
                                })
                                .defaultIfEmpty(ChatRoomResponse.of(room, "Direct Chat"));
                    }
                });
    }

    public Mono<ChatRoomResponse> getChatDetails(UUID currentUserId, UUID chatRoomId) {
        return memberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER));
                    }
                    return chatRoomRepository.findById(chatRoomId)
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND)))
                            .flatMap(room -> {
                                if (room.getType() == ChatRoomType.GROUP) {
                                    return groupChatRepository.findByChatRoomId(room.getId())
                                            .map(group -> ChatRoomResponse.of(room, group.getName()));
                                }
                                return directChatRepository.findByChatRoomId(room.getId())
                                        .flatMap(direct -> {
                                            UUID other = direct.getUser1Id().equals(currentUserId) ? direct.getUser2Id() : direct.getUser1Id();
                                            return userRepository.findById(other)
                                                    .map(u -> ChatRoomResponse.of(room, u.getUsername()));
                                        });
                            });
                });
    }

    public Flux<ChatMemberResponse> getChatMembers(UUID currentUserId, UUID chatRoomId) {
        return memberRepository.existsByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .flatMapMany(isMember -> {
                    if (!isMember) {
                        return Flux.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER));
                    }
                    return memberRepository.findByChatRoomId(chatRoomId)
                            .flatMap(member -> userRepository.findById(member.getUserId())
                                    .map(user -> new ChatMemberResponse(
                                            user.getId(),
                                            user.getUsername(),
                                            member.getRole(),
                                            member.getJoinedAt()
                                    )));
                });
    }

    public Mono<ChatMemberResponse> addMemberToGroup(UUID currentUserId, UUID chatRoomId, AddGroupMemberRequest request) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(requester -> {
                    if (requester.getRole() == GroupRole.MEMBER) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                    }

                    return memberRepository.existsByChatRoomIdAndUserId(chatRoomId, request.userId())
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new BusinessException(ErrorCode.ALREADY_A_MEMBER));
                                }

                                return privacyService.canUserAddToGroup(currentUserId, request.userId())
                                        .flatMap(allowed -> {
                                            if (!allowed) {
                                                return Mono.error(new BusinessException(ErrorCode.PRIVACY_RESTRICTION));
                                            }

                                            return userRepository.findByIdAndDeletedAtIsNull(request.userId())
                                                    .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                                                    .flatMap(user -> {
                                                        ChatRoomMember member = ChatRoomMember.builder()
                                                                .chatRoomId(chatRoomId)
                                                                .userId(user.getId())
                                                                .role(request.role() != null ? request.role() : GroupRole.MEMBER)
                                                                .joinedAt(Instant.now())
                                                                .isNewRecord(true)
                                                                .build();

                                                        return memberRepository.save(member)
                                                                .map(saved -> new ChatMemberResponse(
                                                                        user.getId(),
                                                                        user.getUsername(),
                                                                        saved.getRole(),
                                                                        saved.getJoinedAt()
                                                                ));
                                                    });
                                        });
                            });
                });
    }

    public Mono<ChatMemberResponse> updateMemberRole(UUID currentUserId, UUID chatRoomId, UUID targetUserId, UpdateMemberRoleRequest request) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(requester -> {
                    if (requester.getRole() != GroupRole.OWNER) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                    }

                    return memberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                            .flatMap(targetMember -> {
                                targetMember.setRole(request.role());
                                return memberRepository.save(targetMember)
                                        .flatMap(saved -> userRepository.findById(saved.getUserId())
                                                .map(u -> new ChatMemberResponse(u.getId(), u.getUsername(), saved.getRole(), saved.getJoinedAt())));
                            });
                });
    }

    public Mono<Void> removeMember(UUID currentUserId, UUID chatRoomId, UUID targetUserId) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(requester -> memberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                        .flatMap(targetMember -> {
                            if (requester.getRole() == GroupRole.MEMBER) {
                                return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                            }
                            if (requester.getRole() == GroupRole.ADMIN && (targetMember.getRole() == GroupRole.ADMIN || targetMember.getRole() == GroupRole.OWNER)) {
                                return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS));
                            }
                            return memberRepository.deleteByChatRoomIdAndUserId(chatRoomId, targetUserId).then();
                        }));
    }

    public Mono<Void> leaveGroup(UUID currentUserId, UUID chatRoomId) {
        return memberRepository.findByChatRoomIdAndUserId(chatRoomId, currentUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_A_CHAT_MEMBER)))
                .flatMap(member -> {
                    if (member.getRole() == GroupRole.OWNER) {
                        return memberRepository.countByChatRoomId(chatRoomId)
                                .flatMap(count -> {
                                    if (count > 1) {
                                        // Auto-transfer ownership to first admin or oldest member
                                        return memberRepository.findByChatRoomId(chatRoomId)
                                                .filter(m -> !m.getUserId().equals(currentUserId))
                                                .next()
                                                .flatMap(newOwner -> {
                                                    newOwner.setRole(GroupRole.OWNER);
                                                    return memberRepository.save(newOwner);
                                                })
                                                .then(memberRepository.deleteByChatRoomIdAndUserId(chatRoomId, currentUserId))
                                                .then();
                                    }
                                    return memberRepository.deleteByChatRoomIdAndUserId(chatRoomId, currentUserId).then();
                                });
                    }
                    return memberRepository.deleteByChatRoomIdAndUserId(chatRoomId, currentUserId).then();
                });
    }
}

