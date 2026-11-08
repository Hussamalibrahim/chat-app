package com.chatapp.chat_backend.service.privacy;

import com.chatapp.chat_backend.dto.request.UpdatePrivacySettingsRequest;
import com.chatapp.chat_backend.dto.response.BlockedUserResponse;
import com.chatapp.chat_backend.dto.response.PrivacySettingsResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.block.UserBlock;
import com.chatapp.chat_backend.model.privacy.UserPrivacySetting;
import com.chatapp.chat_backend.model.privacy.WhoCanAddToGroups;
import com.chatapp.chat_backend.model.privacy.WhoCanMessage;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.block.UserBlockRepository;
import com.chatapp.chat_backend.repository.contact.ContactRepository;
import com.chatapp.chat_backend.repository.privacy.UserPrivacySettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final UserPrivacySettingRepository privacySettingRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public Mono<PrivacySettingsResponse> getPrivacySettings(UUID userId) {
        return privacySettingRepository.findByUserId(userId)
                .defaultIfEmpty(UserPrivacySetting.builder()
                        .userId(userId)
                        .whoCanMessage(WhoCanMessage.EVERYONE)
                        .whoCanAddToGroups(WhoCanAddToGroups.EVERYONE)
                        .updatedAt(Instant.now())
                        .build())
                .map(setting -> new PrivacySettingsResponse(
                        setting.getUserId(),
                        setting.getWhoCanMessage(),
                        setting.getWhoCanAddToGroups(),
                        setting.getUpdatedAt()
                ));
    }

    public Mono<PrivacySettingsResponse> updatePrivacySettings(UUID userId, UpdatePrivacySettingsRequest request) {
        return privacySettingRepository.findByUserId(userId)
                .defaultIfEmpty(UserPrivacySetting.builder()
                        .userId(userId)
                        .isNewRecord(true)
                        .whoCanMessage(WhoCanMessage.EVERYONE)
                        .whoCanAddToGroups(WhoCanAddToGroups.EVERYONE)
                        .build())
                .flatMap(setting -> {
                    if (request.whoCanMessage() != null) {
                        setting.setWhoCanMessage(request.whoCanMessage());
                    }
                    if (request.whoCanAddToGroups() != null) {
                        setting.setWhoCanAddToGroups(request.whoCanAddToGroups());
                    }
                    setting.setUpdatedAt(Instant.now());
                    return privacySettingRepository.save(setting);
                })
                .map(setting -> new PrivacySettingsResponse(
                        setting.getUserId(),
                        setting.getWhoCanMessage(),
                        setting.getWhoCanAddToGroups(),
                        setting.getUpdatedAt()
                ));
    }

    public Mono<BlockedUserResponse> blockUser(UUID blockerId, UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            return Mono.error(new BusinessException(ErrorCode.CANNOT_CHAT_WITH_SELF));
        }

        return userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(targetUser -> userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)
                        .flatMap(alreadyBlocked -> {
                            if (alreadyBlocked) {
                                return userBlockRepository.findByBlockerIdAndBlockedId(blockerId, targetUserId)
                                        .map(block -> new BlockedUserResponse(block.getId(), targetUser.getId(), targetUser.getUsername(), block.getCreatedAt()));
                            }

                            UserBlock block = UserBlock.builder()
                                    .blockerId(blockerId)
                                    .blockedId(targetUserId)
                                    .createdAt(Instant.now())
                                    .isNewRecord(true)
                                    .build();

                            return userBlockRepository.save(block)
                                    .map(saved -> new BlockedUserResponse(saved.getId(), targetUser.getId(), targetUser.getUsername(), saved.getCreatedAt()));
                        }));
    }

    public Mono<Void> unblockUser(UUID blockerId, UUID targetUserId) {
        return userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId).then();
    }

    public Flux<BlockedUserResponse> getBlockedUsers(UUID blockerId) {
        return userBlockRepository.findByBlockerId(blockerId)
                .flatMap(block -> userRepository.findById(block.getBlockedId())
                        .map(user -> new BlockedUserResponse(block.getId(), user.getId(), user.getUsername(), block.getCreatedAt())));
    }

    public Mono<Boolean> isBlockedBetween(UUID u1, UUID u2) {
        return userBlockRepository.existsBlockBetween(u1, u2);
    }

    public Mono<Boolean> canUserMessage(UUID senderId, UUID receiverId) {
        return isBlockedBetween(senderId, receiverId)
                .flatMap(blocked -> {
                    if (blocked) {
                        return Mono.just(false);
                    }
                    return privacySettingRepository.findByUserId(receiverId)
                            .map(UserPrivacySetting::getWhoCanMessage)
                            .defaultIfEmpty(WhoCanMessage.EVERYONE)
                            .flatMap(setting -> {
                                if (setting == WhoCanMessage.EVERYONE) {
                                    return Mono.just(true);
                                }
                                return contactRepository.existsByUserIdAndContactUserId(receiverId, senderId);
                            });
                });
    }

    public Mono<Boolean> canUserAddToGroup(UUID inviterId, UUID inviteeId) {
        return isBlockedBetween(inviterId, inviteeId)
                .flatMap(blocked -> {
                    if (blocked) {
                        return Mono.just(false);
                    }
                    return privacySettingRepository.findByUserId(inviteeId)
                            .map(UserPrivacySetting::getWhoCanAddToGroups)
                            .defaultIfEmpty(WhoCanAddToGroups.EVERYONE)
                            .flatMap(setting -> {
                                if (setting == WhoCanAddToGroups.EVERYONE) {
                                    return Mono.just(true);
                                }
                                if (setting == WhoCanAddToGroups.NOBODY) {
                                    return Mono.just(false);
                                }
                                return contactRepository.existsByUserIdAndContactUserId(inviteeId, inviterId);
                            });
                });
    }
}

