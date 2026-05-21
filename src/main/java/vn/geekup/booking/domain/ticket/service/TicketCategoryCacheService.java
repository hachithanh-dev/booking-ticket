package vn.geekup.booking.domain.ticket.service;

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
import vn.geekup.booking.domain.concert.dto.TicketCategoryResponse;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;
import vn.geekup.booking.domain.ticket.repository.TicketCategoryRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCategoryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    private static final String TICKET_STATIC_KEY = "ticket:static:";
    private static final String TICKET_QTY_KEY = "ticket:qty:";
    private static final String TICKET_LOCK_KEY = "lock:ticket:";

    /**
     * Get ticket category detail with static/dynamic cache split.
     * <p>
     * Static cache: name, price, totalQuantity, currency, status (TTL 10m)
     * Dynamic cache: availableQuantity integer (TTL 10m, invalidated on booking)
     * <p>
     * Flow:
     * 1. MGET static + dynamic → 1 round-trip
     * 2. Both HIT → merge → return
     * 3. Any MISS → Redisson lock → double-check → query DB → set both
     */
    @Transactional(readOnly = true)
    public TicketCategoryResponse getTicketCategoryCached(UUID ticketCategoryId) {
        String staticKey = TICKET_STATIC_KEY + ticketCategoryId;
        String dynamicKey = TICKET_QTY_KEY + ticketCategoryId;

        List<String> values = redisTemplate.opsForValue()
                .multiGet(List.of(staticKey, dynamicKey));

        String staticJson = (values != null) ? values.get(0) : null;
        String dynamicQty = (values != null) ? values.get(1) : null;

        if (staticJson != null && dynamicQty != null) {
            log.debug("Cache HIT for ticket category: {}", ticketCategoryId);
            return mergeResponse(staticJson, Integer.parseInt(dynamicQty));
        }

        RLock lock = redissonClient.getLock(TICKET_LOCK_KEY + ticketCategoryId);
        lock.lock();
        try {
            values = redisTemplate.opsForValue()
                    .multiGet(List.of(staticKey, dynamicKey));
            staticJson = (values != null) ? values.get(0) : null;
            dynamicQty = (values != null) ? values.get(1) : null;

            if (staticJson != null && dynamicQty != null) {
                log.debug("Cache HIT after double-check for ticket: {}", ticketCategoryId);
                return mergeResponse(staticJson, Integer.parseInt(dynamicQty));
            }

            TicketCategory tc = ticketCategoryRepository.findById(ticketCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket category not found: " + ticketCategoryId));

            TicketCategoryResponse response = new TicketCategoryResponse(
                    tc.getId(), tc.getName(), tc.getPriceAmount(),
                    tc.getCurrency(), tc.getTotalQuantity(),
                    tc.getAvailableQuantity(), tc.getStatus().name());

            redisTemplate.opsForValue().set(staticKey, serialize(response),
                    Duration.ofSeconds(cacheProperties.getTicketStaticTtlSeconds()));
            redisTemplate.opsForValue().set(dynamicKey,
                    String.valueOf(tc.getAvailableQuantity()),
                    Duration.ofSeconds(cacheProperties.getTicketDynamicTtlSeconds()));
            log.debug("Cache SET for ticket category: {}", ticketCategoryId);

            return response;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Invalidate dynamic cache only — called from AFTER_COMMIT listener.
     */
    public void invalidateDynamic(UUID ticketCategoryId) {
        redisTemplate.delete(TICKET_QTY_KEY + ticketCategoryId);
        log.debug("Dynamic cache invalidated for ticket: {}", ticketCategoryId);
    }

    private TicketCategoryResponse mergeResponse(String staticJson, int dynamicQty) {
        TicketCategoryResponse cached = deserialize(staticJson, TicketCategoryResponse.class);
        return new TicketCategoryResponse(
                cached.id(), cached.name(), cached.priceAmount(),
                cached.currency(), cached.totalQuantity(),
                dynamicQty, cached.status());
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
