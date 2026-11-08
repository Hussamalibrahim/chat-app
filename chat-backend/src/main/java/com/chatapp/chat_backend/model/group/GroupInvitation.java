package com.chatapp.chat_backend.model.group;

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
@Table("group_invitations")
public class GroupInvitation implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("group_chat_id")
    private UUID groupChatId;

    @Column("inviter_id")
    private UUID inviterId;

    @Column("invitee_id")
    private UUID inviteeId;

    private InvitationStatus status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return id == null || isNewRecord;
    }
}

