package vn.geekup.booking.domain.booking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.geekup.booking.domain.ticket.service.TicketCategoryCacheService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketCacheInvalidationListener {

    private final TicketCategoryCacheService ticketCategoryCacheService;

    /**
     * AFTER_COMMIT: fires only after DB transaction committed successfully.
     * <p>
     * CRITICAL: Must NEVER throw exception — Redis failure here would propagate
     * back to the caller as HTTP 500, even though DB already committed.
     * DB committed = business success → user must see 201, not 500.
     * Cache miss is acceptable (self-heals on next read), but 500 is not.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketQuantityChanged(TicketQuantityChangedEvent event) {
        for (UUID ticketCategoryId : event.ticketCategoryIds()) {
            try {
                ticketCategoryCacheService.invalidateDynamic(ticketCategoryId);
            } catch (Exception e) {
                log.error("Failed to invalidate ticket cache for {}: {}",
                        ticketCategoryId, e.getMessage());
            }
        }
        log.debug("Ticket dynamic cache invalidation processed for: {}", event.ticketCategoryIds());
    }
}
