package com.chatapp.chat_backend.config;

import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.message.MessageRepository;
import com.chatapp.chat_backend.repository.report.UserReportRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BusinessMetricsService {

        private final MeterRegistry meterRegistry;
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;
        private final ChatRoomRepository chatRoomRepository;
        private final UserReportRepository reportRepository;

        private final AtomicLong totalUsers = new AtomicLong(0);
        private final AtomicLong activeUsers24h = new AtomicLong(0);
        private final AtomicLong totalMessages = new AtomicLong(0);
        private final AtomicLong totalChatRooms = new AtomicLong(0);
        private final AtomicLong groupChatRooms = new AtomicLong(0);
        private final AtomicLong directChatRooms = new AtomicLong(0);
        private final AtomicLong totalReports = new AtomicLong(0);
        private final AtomicLong pendingReports = new AtomicLong(0);

        @PostConstruct
        public void init() {
                Gauge.builder("chat_users_total", totalUsers, AtomicLong::get)
                                .description("Total registered users in the chat system")
                                .register(meterRegistry);

                Gauge.builder("chat_users_active_24h", activeUsers24h, AtomicLong::get)
                                .description("Active users who sent messages in the last 24 hours")
                                .register(meterRegistry);

                Gauge.builder("chat_messages_total", totalMessages, AtomicLong::get)
                                .description("Total messages stored")
                                .register(meterRegistry);

                Gauge.builder("chat_rooms_total", totalChatRooms, AtomicLong::get)
                                .description("Total chat rooms")
                                .register(meterRegistry);

                Gauge.builder("chat_rooms_group_total", groupChatRooms, AtomicLong::get)
                                .description("Total group chat rooms")
                                .register(meterRegistry);

                Gauge.builder("chat_rooms_direct_total", directChatRooms, AtomicLong::get)
                                .description("Total direct chat rooms")
                                .register(meterRegistry);

                Gauge.builder("chat_reports_total", totalReports, AtomicLong::get)
                                .description("Total submitted user reports")
                                .register(meterRegistry);

                Gauge.builder("chat_reports_pending", pendingReports, AtomicLong::get)
                                .description("Total pending user reports awaiting review")
                                .register(meterRegistry);

                // Initial refresh
                refreshMetrics();
        }

        @Scheduled(fixedRate = 60000)
        public void refreshMetrics() {
                Instant since24h = Instant.now().minus(Duration.ofDays(1));

                userRepository.countTotalActiveUsers()
                                .doOnNext(totalUsers::set)
                                .subscribe();

                userRepository.countActiveMessengersSince(since24h)
                                .doOnNext(activeUsers24h::set)
                                .subscribe();

                messageRepository.countTotalMessages()
                                .doOnNext(totalMessages::set)
                                .subscribe();

                chatRoomRepository.countTotalRooms()
                                .doOnNext(totalChatRooms::set)
                                .subscribe();

                chatRoomRepository.countGroupRooms()
                                .doOnNext(groupChatRooms::set)
                                .subscribe();

                chatRoomRepository.countDirectRooms()
                                .doOnNext(directChatRooms::set)
                                .subscribe();

                reportRepository.countTotalReports()
                                .doOnNext(totalReports::set)
                                .subscribe();

                reportRepository.countPendingReports()
                                .doOnNext(pendingReports::set)
                                .subscribe();
        }
}
