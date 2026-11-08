package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.CreateReportRequest;
import com.chatapp.chat_backend.dto.request.ReviewReportRequest;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.model.report.ReportDecision;
import com.chatapp.chat_backend.model.report.UserReport;
import com.chatapp.chat_backend.model.report.UserReportReview;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.report.UserReportRepository;
import com.chatapp.chat_backend.repository.report.UserReportReviewRepository;
import com.chatapp.chat_backend.service.report.ReportService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private UUID reporterId;
    private UUID reportedId;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        reportedId = UUID.randomUUID();
    }

    @Test
    void createReport_CannotReportSelf() {
        ReportService reportService = new ReportService(
                TestStubs.of(UserReportRepository.class).build(),
                TestStubs.of(UserReportReviewRepository.class).build(),
                TestStubs.of(UserRepository.class).build()
        );

        StepVerifier.create(reportService.createReport(reporterId, new CreateReportRequest(reporterId, null, "Spam", "Self")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_REPORT_SELF)
                .verify();
    }

    @Test
    void createReport_Success() {
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findByIdAndDeletedAtIsNull", args -> Mono.just(User.builder().id(reportedId).username("spammer").build()))
                .on("findById", args -> {
                    UUID id = (UUID) args[0];
                    if (id.equals(reporterId)) return Mono.just(User.builder().id(reporterId).username("reporter").build());
                    return Mono.just(User.builder().id(reportedId).username("spammer").build());
                })
                .build();

        UUID reportId = UUID.randomUUID();
        UserReport saved = UserReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedId)
                .reason("Spam")
                .details("Harassment")
                .createdAt(Instant.now())
                .build();

        UserReportRepository reportRepo = TestStubs.of(UserReportRepository.class)
                .on("save", args -> Mono.just(saved))
                .build();

        UserReportReviewRepository reviewRepo = TestStubs.of(UserReportReviewRepository.class)
                .on("findByReportId", args -> Mono.empty())
                .build();

        ReportService reportService = new ReportService(reportRepo, reviewRepo, userRepo);

        StepVerifier.create(reportService.createReport(reporterId, new CreateReportRequest(reportedId, null, "Spam", "Harassment")))
                .assertNext(res -> {
                    assertEquals(reportId, res.id());
                    assertEquals("Spam", res.reason());
                    assertEquals("spammer", res.reportedUsername());
                })
                .verifyComplete();
    }

    @Test
    void reviewReport_Success() {
        UUID reportId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UserReport report = UserReport.builder().id(reportId).reporterId(reporterId).reportedUserId(reportedId).reason("Spam").createdAt(Instant.now()).build();

        UserReportRepository reportRepo = TestStubs.of(UserReportRepository.class)
                .on("findById", args -> Mono.just(report))
                .build();

        UserReportReview review = UserReportReview.builder().id(UUID.randomUUID()).reportId(reportId).adminId(adminId).decision(ReportDecision.USER_SUSPENDED).adminNotes("Action taken").reviewedAt(Instant.now()).build();
        UserReportReviewRepository reviewRepo = TestStubs.of(UserReportReviewRepository.class)
                .on("findByReportId", args -> Mono.just(review))
                .on("save", args -> Mono.just(review))
                .build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findById", args -> {
                    UUID id = (UUID) args[0];
                    if (id.equals(reporterId)) return Mono.just(User.builder().id(reporterId).username("reporter").build());
                    return Mono.just(User.builder().id(reportedId).username("spammer").build());
                })
                .build();

        ReportService reportService = new ReportService(reportRepo, reviewRepo, userRepo);

        StepVerifier.create(reportService.reviewReport(adminId, reportId, new ReviewReportRequest(ReportDecision.USER_SUSPENDED, "Action taken")))
                .assertNext(res -> {
                    assertTrue(res.reviewed());
                    assertEquals(ReportDecision.USER_SUSPENDED, res.decision());
                    assertEquals("Action taken", res.adminNotes());
                })
                .verifyComplete();
    }
}
