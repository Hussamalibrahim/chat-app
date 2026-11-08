package com.chatapp.chat_backend.service.admin;

import com.chatapp.chat_backend.dto.response.AdminDashboardStatsResponse;
import com.chatapp.chat_backend.dto.response.AdminUserDetailResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.message.MessageRepository;
import com.chatapp.chat_backend.repository.report.UserReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserReportRepository reportRepository;

    public Mono<AdminDashboardStatsResponse> getDashboardStats() {
        Instant since24Hours = Instant.now().minus(Duration.ofDays(1));
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);

        return Mono.zip(
                args -> new AdminDashboardStatsResponse(
                        (Long) args[0],
                        (Long) args[1],
                        (Long) args[2],
                        (Long) args[3],
                        (Long) args[4],
                        (Long) args[5],
                        (Long) args[6],
                        (Long) args[7],
                        (Long) args[8]),
                userRepository.countTotalActiveUsers().defaultIfEmpty(0L),
                userRepository.countActiveMessengersSince(since24Hours).defaultIfEmpty(0L),
                messageRepository.countTotalMessages().defaultIfEmpty(0L),
                messageRepository.countMessagesSince(startOfToday).defaultIfEmpty(0L),
                chatRoomRepository.countTotalRooms().defaultIfEmpty(0L),
                chatRoomRepository.countGroupRooms().defaultIfEmpty(0L),
                chatRoomRepository.countDirectRooms().defaultIfEmpty(0L),
                reportRepository.countTotalReports().defaultIfEmpty(0L),
                reportRepository.countPendingReports().defaultIfEmpty(0L));
    }

    public Flux<AdminUserDetailResponse> listUsers(int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);
        return userRepository.findAllBy(PageRequest.of(pageIndex, pageSize))
                .map(AdminUserDetailResponse::from);
    }

    public Flux<AdminUserDetailResponse> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return Flux.empty();
        }

        return userRepository.findByUsernameContainingIgnoreCaseAndDeletedAtIsNull(query.trim())
                .map(AdminUserDetailResponse::from);
    }

    public Mono<AdminUserDetailResponse> getUserDetails(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> user.getDeletedAt() == null
                        ? Mono.just(AdminUserDetailResponse.from(user))
                        : Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }

    public Mono<AdminUserDetailResponse> suspendUser(UUID userId, boolean suspend) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    user.setDeletedAt(suspend ? Instant.now() : null);
                    user.setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                })
                .map(AdminUserDetailResponse::from);
    }
}
