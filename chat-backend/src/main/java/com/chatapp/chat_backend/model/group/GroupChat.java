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
@Table("group_chats")
public class GroupChat implements Persistable<UUID> {

    @Id
    @Column("chat_room_id")
    private UUID chatRoomId;

    private String name;

    private String description;

    @Column("created_by")
    private UUID createdBy;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public UUID getId() {
        return chatRoomId;
    }

    @Override
    public boolean isNew() {
        return isNewRecord;
    }
}

