package vn.geekup.booking.domain.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.geekup.booking.config.CacheProperties;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingLockService {

    private final StringRedisTemplate redisTemplate;
    private final CacheProperties cacheProperties;

    private static final String BOOKING_LOCK_KEY = "booking:lock:";

    /**
     * Acquire idempotency lock using SETNX.
     * Key: booking:lock:{userId}:{idempotencyKey}
     * TTL: 30s
     *
     * @return true if lock acquired (first request), false if duplicate
     */
    public boolean acquireIdempotencyLock(UUID userId, String idempotencyKey) {
        String key = BOOKING_LOCK_KEY + userId + ":" + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING",
                        Duration.ofSeconds(cacheProperties.getBookingLockTtlSeconds())));
    }

    /**
     * Release lock on failure — allows retry with same idempotency key.
     */
    public void releaseIdempotencyLock(UUID userId, String idempotencyKey) {
        String key = BOOKING_LOCK_KEY + userId + ":" + idempotencyKey;
        redisTemplate.delete(key);
    }
}
