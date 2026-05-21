package vn.geekup.booking.domain.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveBookingItemRequest(
        @NotNull(message = "ticketCategoryId is required")
        UUID ticketCategoryId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
