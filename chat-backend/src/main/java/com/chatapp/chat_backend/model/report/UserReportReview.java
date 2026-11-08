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
@Table("user_report_reviews")
public class UserReportReview implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("report_id")
    private UUID reportId;

    @Column("admin_id")
    private UUID adminId;

    private ReportDecision decision;

    @Column("admin_notes")
    private String adminNotes;

    @Column("reviewed_at")
    private Instant reviewedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return id == null || isNewRecord;
    }
}

