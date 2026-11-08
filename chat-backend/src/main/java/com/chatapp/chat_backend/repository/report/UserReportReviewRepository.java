package com.chatapp.chat_backend.repository.report;

import com.chatapp.chat_backend.model.report.UserReportReview;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserReportReviewRepository extends ReactiveCrudRepository<UserReportReview, UUID> {

    Mono<UserReportReview> findByReportId(UUID reportId);

    Mono<Boolean> existsByReportId(UUID reportId);
}

