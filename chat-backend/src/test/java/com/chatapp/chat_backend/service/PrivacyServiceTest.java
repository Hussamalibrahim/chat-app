package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.UpdatePrivacySettingsRequest;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.block.UserBlock;
import com.chatapp.chat_backend.model.privacy.UserPrivacySetting;
import com.chatapp.chat_backend.model.privacy.WhoCanAddToGroups;
import com.chatapp.chat_backend.model.privacy.WhoCanMessage;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.block.UserBlockRepository;
import com.chatapp.chat_backend.repository.contact.ContactRepository;
import com.chatapp.chat_backend.repository.privacy.UserPrivacySettingRepository;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrivacyServiceTest {

    private UUID userId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    @Test
    void getPrivacySettings_DefaultEveryone() {
        UserPrivacySettingRepository settingRepo = TestStubs.of(UserPrivacySettingRepository.class)
                .on("findByUserId", args -> Mono.empty())
                .build();

        PrivacyService privacyService = new PrivacyService(
                settingRepo,
                TestStubs.of(UserBlockRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                TestStubs.of(ContactRepository.class).build()
        );

        StepVerifier.create(privacyService.getPrivacySettings(userId))
                .assertNext(res -> {
                    assertEquals(userId, res.userId());
                    assertEquals(WhoCanMessage.EVERYONE, res.whoCanMessage());
                    assertEquals(WhoCanAddToGroups.EVERYONE, res.whoCanAddToGroups());
                })
                .verifyComplete();
    }

    @Test
    void updatePrivacySettings_Success() {
        UserPrivacySetting saved = UserPrivacySetting.builder()
                .userId(userId)
                .whoCanMessage(WhoCanMessage.CONTACTS)
                .whoCanAddToGroups(WhoCanAddToGroups.NOBODY)
                .updatedAt(Instant.now())
                .build();

        UserPrivacySettingRepository settingRepo = TestStubs.of(UserPrivacySettingRepository.class)
                .on("findByUserId", args -> Mono.empty())
                .on("save", args -> Mono.just(saved))
                .build();

        PrivacyService privacyService = new PrivacyService(
                settingRepo,
                TestStubs.of(UserBlockRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                TestStubs.of(ContactRepository.class).build()
        );

        StepVerifier.create(privacyService.updatePrivacySettings(userId, new UpdatePrivacySettingsRequest(WhoCanMessage.CONTACTS, WhoCanAddToGroups.NOBODY)))
                .assertNext(res -> {
                    assertEquals(WhoCanMessage.CONTACTS, res.whoCanMessage());
                    assertEquals(WhoCanAddToGroups.NOBODY, res.whoCanAddToGroups());
                })
                .verifyComplete();
    }

    @Test
    void blockUser_Success() {
        User target = User.builder().id(targetUserId).username("charlie").build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(target))
                .build();

        UserBlock block = UserBlock.builder().id(UUID.randomUUID()).blockerId(userId).blockedId(targetUserId).createdAt(Instant.now()).build();
        UserBlockRepository blockRepo = TestStubs.of(UserBlockRepository.class)
                .on("existsByBlockerIdAndBlockedId", args -> Mono.just(false))
                .on("save", args -> Mono.just(block))
                .build();

        PrivacyService privacyService = new PrivacyService(
                TestStubs.of(UserPrivacySettingRepository.class).build(),
                blockRepo,
                userRepo,
                TestStubs.of(ContactRepository.class).build()
        );

        StepVerifier.create(privacyService.blockUser(userId, targetUserId))
                .assertNext(res -> {
                    assertEquals(targetUserId, res.blockedUserId());
                    assertEquals("charlie", res.blockedUsername());
                })
                .verifyComplete();
    }

    @Test
    void canUserMessage_Blocked_ReturnsFalse() {
        UserBlockRepository blockRepo = TestStubs.of(UserBlockRepository.class)
                .on("existsBlockBetween", args -> Mono.just(true))
                .build();

        PrivacyService privacyService = new PrivacyService(
                TestStubs.of(UserPrivacySettingRepository.class).build(),
                blockRepo,
                TestStubs.of(UserRepository.class).build(),
                TestStubs.of(ContactRepository.class).build()
        );

        StepVerifier.create(privacyService.canUserMessage(userId, targetUserId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void canUserMessage_ContactsOnly_NotInContacts_ReturnsFalse() {
        UserBlockRepository blockRepo = TestStubs.of(UserBlockRepository.class)
                .on("existsBlockBetween", args -> Mono.just(false))
                .build();

        UserPrivacySetting setting = UserPrivacySetting.builder().whoCanMessage(WhoCanMessage.CONTACTS).build();
        UserPrivacySettingRepository settingRepo = TestStubs.of(UserPrivacySettingRepository.class)
                .on("findByUserId", args -> Mono.just(setting))
                .build();

        ContactRepository contactRepo = TestStubs.of(ContactRepository.class)
                .on("existsByUserIdAndContactUserId", args -> Mono.just(false))
                .build();

        PrivacyService privacyService = new PrivacyService(
                settingRepo,
                blockRepo,
                TestStubs.of(UserRepository.class).build(),
                contactRepo
        );

        StepVerifier.create(privacyService.canUserMessage(userId, targetUserId))
                .expectNext(false)
                .verifyComplete();
    }
}
