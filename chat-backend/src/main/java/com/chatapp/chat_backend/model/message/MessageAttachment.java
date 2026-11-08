package com.chatapp.chat_backend.model.message;

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
@Table("message_attachments")
public class MessageAttachment implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("message_id")
    private UUID messageId;

    @Column("file_name")
    private String fileName;

    @Column("file_type")
    private String fileType;

    @Column("file_size")
    private Long fileSize;

    @Column("file_url")
    private String fileUrl;

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

