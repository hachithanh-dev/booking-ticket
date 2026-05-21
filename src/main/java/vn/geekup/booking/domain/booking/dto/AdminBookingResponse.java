package vn.geekup.booking.domain.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminBookingResponse(
        UUID id,
        UUID userId,
        UUID concertId,
        String status,
        long subtotalAmount,
        long discountAmount,
        long totalAmount,
        String currency,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
