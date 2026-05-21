package vn.geekup.booking.domain.concert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.geekup.booking.common.exception.ResourceNotFoundException;
import vn.geekup.booking.config.CacheProperties;
import vn.geekup.booking.domain.concert.dto.ConcertDetailResponse;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.concert.mapper.ConcertMapper;
import vn.geekup.booking.domain.concert.repository.ConcertRepository;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ConcertRepository concertRepository;
    private final ConcertMapper concertMapper;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    private static final String CONCERT_DETAIL_KEY = "concert:detail:";
    private static final String CONCERT_LOCK_KEY = "lock:concert:detail:";


    @Transactional(readOnly = true)
    public ConcertDetailResponse getConcertDetailCached(UUID concertId) {
        String cacheKey = CONCERT_DETAIL_KEY + concertId;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for concert detail: {}", concertId);
            return deserialize(cached, ConcertDetailResponse.class);
        }

        RLock lock = redissonClient.getLock(CONCERT_LOCK_KEY + concertId);
        lock.lock();
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache HIT after double-check for concert: {}", concertId);
                return deserialize(cached, ConcertDetailResponse.class);
            }

            Concert concert = concertRepository.findDetailById(concertId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Concert not found: " + concertId));
            ConcertDetailResponse response = concertMapper.toDetailResponse(concert);

            redisTemplate.opsForValue().set(cacheKey,
                    serialize(response),
                    Duration.ofSeconds(cacheProperties.getConcertTtlSeconds()));
            log.debug("Cache SET for concert detail: {}", concertId);

            return response;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cache value", e);
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cache value", e);
        }
    }
}
