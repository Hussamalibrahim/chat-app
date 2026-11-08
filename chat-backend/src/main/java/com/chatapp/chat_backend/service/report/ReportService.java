package com.chatapp.chat_backend.service.report;

import com.chatapp.chat_backend.dto.request.CreateReportRequest;
import com.chatapp.chat_backend.dto.request.ReviewReportRequest;
import com.chatapp.chat_backend.dto.response.ReportResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.report.UserReport;
import com.chatapp.chat_backend.model.report.UserReportReview;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.report.UserReportRepository;
import com.chatapp.chat_backend.repository.report.UserReportReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserReportRepository reportRepository;
    private final UserReportReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public Mono<ReportResponse> createReport(UUID currentUserId, CreateReportRequest request) {
        if (currentUserId.equals(request.reportedUserId())) {
            return Mono.error(new BusinessException(ErrorCode.CANNOT_REPORT_SELF));
        }

        return userRepository.findByIdAndDeletedAtIsNull(request.reportedUserId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(reportedUser -> {
                    Instant now = Instant.now();
                    UserReport report = UserReport.builder()
                            .reporterId(currentUserId)
                            .reportedUserId(request.reportedUserId())
                            .messageId(request.messageId())
                            .reason(request.reason().trim())
                            .details(request.details() != null ? request.details().trim() : null)
                            .createdAt(now)
                            .isNewRecord(true)
                            .build();

                    return reportRepository.save(report)
                            .flatMap(this::mapToResponse);
                });
    }

    public Flux<ReportResponse> getReports(String reason, int page, int size) {
        int limit = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * limit;

        return reportRepository.findFilteredReports(reason, limit, offset)
                .flatMap(this::mapToResponse);
    }

    public Mono<ReportResponse> getReportById(UUID reportId) {
        return reportRepository.findById(reportId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.REPORT_NOT_FOUND)))
                .flatMap(this::mapToResponse);
    }

    @Transactional
    public Mono<ReportResponse> reviewReport(UUID adminId, UUID reportId, ReviewReportRequest request) {
        return reportRepository.findById(reportId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.REPORT_NOT_FOUND)))
                .flatMap(report -> reviewRepository.findByReportId(reportId)
                        .defaultIfEmpty(UserReportReview.builder()
                                .reportId(reportId)
                                .adminId(adminId)
                                .isNewRecord(true)
                                .build())
                        .flatMap(review -> {
                            review.setAdminId(adminId);
                            review.setDecision(request.decision());
                            review.setAdminNotes(request.adminNotes());
                            review.setReviewedAt(Instant.now());

                            return reviewRepository.save(review)
                                    .then(mapToResponse(report));
                        }));
    }

    private Mono<ReportResponse> mapToResponse(UserReport report) {
        return Mono.zip(
                userRepository.findById(report.getReporterId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown"),
                userRepository.findById(report.getReportedUserId()).map(u -> u.getUsername()).defaultIfEmpty("Unknown"),
                reviewRepository.findByReportId(report.getId()).map(java.util.Optional::of).defaultIfEmpty(java.util.Optional.empty())
        ).map(tuple -> {
            UserReportReview rev = tuple.getT3().orElse(null);
            return new ReportResponse(
                    report.getId(),
                    report.getReporterId(),
                    tuple.getT1(),
                    report.getReportedUserId(),
                    tuple.getT2(),
                    report.getMessageId(),
                    report.getReason(),
                    report.getDetails(),
                    rev != null,
                    rev != null ? rev.getDecision() : null,
                    rev != null ? rev.getAdminNotes() : null,
                    rev != null ? rev.getReviewedAt() : null,
                    report.getCreatedAt()
            );
        });
    }
}
