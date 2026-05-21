package vn.geekup.booking.domain.booking.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingDetailResponse(
        UUID id,
        String status,
        long subtotalAmount,
        long discountAmount,
        long totalAmount,
        String currency,
        LocalDateTime expiresAt,
        List<BookingItemResponse> items
) {
}
