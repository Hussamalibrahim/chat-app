package com.chatapp.chat_backend.repository.report;

import com.chatapp.chat_backend.model.report.UserReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserReportRepository extends ReactiveCrudRepository<UserReport, UUID> {

    Flux<UserReport> findByReportedUserId(UUID reportedUserId);

    Flux<UserReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(*) FROM user_reports")
    Mono<Long> countTotalReports();

    @Query("""
        SELECT COUNT(*) FROM user_reports ur
        WHERE NOT EXISTS (
            SELECT 1 FROM user_report_reviews urr WHERE urr.report_id = ur.id
        )
    """)
    Mono<Long> countPendingReports();

    @Query("""
        SELECT ur.* FROM user_reports ur
        WHERE (:reason IS NULL OR ur.reason = :reason)
        ORDER BY ur.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    Flux<UserReport> findFilteredReports(String reason, long limit, long offset);
}

