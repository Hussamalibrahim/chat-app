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
@Table("direct_chats")
public class DirectChat implements Persistable<UUID> {

    @Id
    @Column("chat_room_id")
    private UUID chatRoomId;

    @Column("user1_id")
    private UUID user1Id;

    @Column("user2_id")
    private UUID user2Id;

    @Column("created_at")
    private Instant createdAt;

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

