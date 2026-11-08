package com.chatapp.chat_backend.model.chat;

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
@Table("chat_room_members")
public class ChatRoomMember implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("chat_room_id")
    private UUID chatRoomId;

    @Column("user_id")
    private UUID userId;

    private GroupRole role;

    @Column("joined_at")
    private Instant joinedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return id == null || isNewRecord;
    }
}
