package vn.geekup.booking.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBookingStatusRequest(
        @NotBlank(message = "status is required")
        String status,

        String reason
) {
}
