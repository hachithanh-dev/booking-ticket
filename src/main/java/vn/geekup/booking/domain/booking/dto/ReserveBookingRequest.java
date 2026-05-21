package vn.geekup.booking.domain.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveBookingRequest(
        @NotNull(message = "concertId is required")
        UUID concertId,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<ReserveBookingItemRequest> items,

        String voucherCode
) {
}
