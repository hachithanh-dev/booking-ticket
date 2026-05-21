package vn.geekup.booking.domain.booking.event;

import java.util.List;
import java.util.UUID;

/**
 * Published inside @Transactional methods that change ticket availableQuantity.
 * Listener fires AFTER_COMMIT only — cache not invalidated on rollback.
 */
public record TicketQuantityChangedEvent(List<UUID> ticketCategoryIds) {
}
