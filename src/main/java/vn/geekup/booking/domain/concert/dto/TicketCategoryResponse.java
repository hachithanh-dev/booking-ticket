package vn.geekup.booking.domain.concert.dto;

import java.util.UUID;

public record TicketCategoryResponse(
        UUID id,
        String name,
        long priceAmount,
        String currency,
        int totalQuantity,
        int availableQuantity,
        String status
) {
}
