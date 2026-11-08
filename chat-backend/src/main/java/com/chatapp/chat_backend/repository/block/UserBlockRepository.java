package com.chatapp.chat_backend.repository.block;

import com.chatapp.chat_backend.model.block.UserBlock;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserBlockRepository extends ReactiveCrudRepository<UserBlock, UUID> {

    Flux<UserBlock> findByBlockerId(UUID blockerId);

    Mono<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Mono<Boolean> existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @Query("""
        SELECT COUNT(*) > 0 FROM user_blocks
        WHERE (blocker_id = :u1 AND blocked_id = :u2)
           OR (blocker_id = :u2 AND blocked_id = :u1)
    """)
    Mono<Boolean> existsBlockBetween(UUID u1, UUID u2);

    @Modifying
    @Query("DELETE FROM user_blocks WHERE blocker_id = :blockerId AND blocked_id = :blockedId")
    Mono<Integer> deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}

