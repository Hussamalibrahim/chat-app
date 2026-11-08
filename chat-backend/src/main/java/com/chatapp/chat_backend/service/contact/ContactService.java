package com.chatapp.chat_backend.service.contact;

import com.chatapp.chat_backend.dto.request.CreateContactRequest;
import com.chatapp.chat_backend.dto.response.ContactResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.contact.Contact;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.contact.ContactRepository;
import com.chatapp.chat_backend.service.privacy.PrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final PrivacyService privacyService;

    public Mono<ContactResponse> addContact(UUID userId, CreateContactRequest request) {
        if (userId.equals(request.contactUserId())) {
            return Mono.error(new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_CONTACT));
        }

        return userRepository.findByIdAndDeletedAtIsNull(request.contactUserId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(contactUser -> privacyService.isBlockedBetween(userId, request.contactUserId())
                        .flatMap(blocked -> {
                            if (blocked) {
                                return Mono.error(new BusinessException(ErrorCode.USER_BLOCKED));
                            }

                            return contactRepository.existsByUserIdAndContactUserId(userId, request.contactUserId())
                                    .flatMap(exists -> {
                                        if (exists) {
                                            return Mono.error(new BusinessException(ErrorCode.CONTACT_ALREADY_EXISTS));
                                        }

                                        Contact contact = Contact.builder()
                                                .userId(userId)
                                                .contactUserId(contactUser.getId())
                                                .alias(request.alias() != null ? request.alias().trim() : null)
                                                .createdAt(Instant.now())
                                                .isNewRecord(true)
                                                .build();

                                        return contactRepository.save(contact)
                                                .map(saved -> new ContactResponse(
                                                        saved.getId(),
                                                        contactUser.getId(),
                                                        contactUser.getUsername(),
                                                        contactUser.getEmail(),
                                                        contactUser.getPhone(),
                                                        saved.getAlias(),
                                                        saved.getCreatedAt()
                                                ));
                                    });
                        }));
    }

    public Mono<Void> removeContact(UUID userId, UUID contactUserId) {
        return contactRepository.deleteByUserIdAndContactUserId(userId, contactUserId).then();
    }

    public Flux<ContactResponse> getContacts(UUID userId) {
        return contactRepository.findByUserId(userId)
                .flatMap(contact -> userRepository.findById(contact.getContactUserId())
                        .map(user -> new ContactResponse(
                                contact.getId(),
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getPhone(),
                                contact.getAlias(),
                                contact.getCreatedAt()
                        )));
    }

    public Flux<ContactResponse> searchContacts(UUID userId, String query) {
        String lowerQuery = query.toLowerCase().trim();
        return getContacts(userId)
                .filter(contact -> (contact.contactUsername() != null && contact.contactUsername().toLowerCase().contains(lowerQuery))
                        || (contact.alias() != null && contact.alias().toLowerCase().contains(lowerQuery))
                        || (contact.contactEmail() != null && contact.contactEmail().toLowerCase().contains(lowerQuery))
                        || (contact.contactPhone() != null && contact.contactPhone().contains(lowerQuery)));
    }
}

