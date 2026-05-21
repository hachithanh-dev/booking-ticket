package vn.geekup.booking.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentWebhookRequest(
        @NotNull(message = "bookingId is required")
        UUID bookingId,

        @NotBlank(message = "transactionId is required")
        String transactionId,

        @NotBlank(message = "status is required")
        String status
) {
}
