package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.CreateContactRequest;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.contact.Contact;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.contact.ContactRepository;
import com.chatapp.chat_backend.service.contact.ContactService;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContactServiceTest {

    private UUID userId;
    private UUID contactUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contactUserId = UUID.randomUUID();
    }

    private PrivacyService fakePrivacyService(boolean blocked) {
        return new PrivacyService(null, null, null, null) {
            @Override
            public Mono<Boolean> isBlockedBetween(UUID u1, UUID u2) {
                return Mono.just(blocked);
            }
        };
    }

    @Test
    void addContact_CannotAddSelf() {
        ContactService contactService = new ContactService(
                TestStubs.of(ContactRepository.class).build(),
                TestStubs.of(UserRepository.class).build(),
                fakePrivacyService(false)
        );

        StepVerifier.create(contactService.addContact(userId, new CreateContactRequest(userId, "Me")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_ADD_SELF_AS_CONTACT)
                .verify();
    }

    @Test
    void addContact_AlreadyExists_ThrowsException() {
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(User.builder().id(contactUserId).build()))
                .build();

        ContactRepository contactRepo = TestStubs.of(ContactRepository.class)
                .on("existsByUserIdAndContactUserId", args -> Mono.just(true))
                .build();

        ContactService contactService = new ContactService(contactRepo, userRepo, fakePrivacyService(false));

        StepVerifier.create(contactService.addContact(userId, new CreateContactRequest(contactUserId, "Bob")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CONTACT_ALREADY_EXISTS)
                .verify();
    }

    @Test
    void addContact_Success() {
        User contactUser = User.builder().id(contactUserId).username("bob").email("bob@mail.com").build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(contactUser))
                .build();

        Contact saved = Contact.builder().id(UUID.randomUUID()).userId(userId).contactUserId(contactUserId).alias("Bobby").createdAt(Instant.now()).build();
        ContactRepository contactRepo = TestStubs.of(ContactRepository.class)
                .on("existsByUserIdAndContactUserId", args -> Mono.just(false))
                .on("save", args -> Mono.just(saved))
                .build();

        ContactService contactService = new ContactService(contactRepo, userRepo, fakePrivacyService(false));

        StepVerifier.create(contactService.addContact(userId, new CreateContactRequest(contactUserId, "Bobby")))
                .assertNext(res -> {
                    assertEquals(contactUserId, res.contactUserId());
                    assertEquals("bob", res.contactUsername());
                    assertEquals("Bobby", res.alias());
                })
                .verifyComplete();
    }
}
