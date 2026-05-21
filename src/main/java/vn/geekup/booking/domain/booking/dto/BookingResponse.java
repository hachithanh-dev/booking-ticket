package vn.geekup.booking.domain.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String status,
        long subtotalAmount,
        long discountAmount,
        long totalAmount,
        String currency,
        LocalDateTime expiresAt
) {
}
