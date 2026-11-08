package com.chatapp.chat_backend.model.report;

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
@Table("user_reports")
public class UserReport implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("reporter_id")
    private UUID reporterId;

    @Column("reported_user_id")
    private UUID reportedUserId;

    @Column("message_id")
    private UUID messageId;

    private String reason;

    private String details;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return id == null || isNewRecord;
    }
}

