package com.chatapp.chat_backend.model.privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_privacy_settings")
public class UserPrivacySetting implements Persistable<UUID> {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("who_can_message")
    private WhoCanMessage whoCanMessage;

    @Column("who_can_add_to_groups")
    private WhoCanAddToGroups whoCanAddToGroups;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewRecord;
    }
}

