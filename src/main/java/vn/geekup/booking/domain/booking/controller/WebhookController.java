package vn.geekup.booking.domain.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.geekup.booking.domain.booking.dto.PaymentWebhookRequest;
import vn.geekup.booking.domain.booking.service.BookingService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "Payment gateway webhook callbacks")
public class WebhookController {

    private final BookingService bookingService;

    @PostMapping("/payments")
    @Operation(
            summary = "Process payment webhook",
            description = "Receives payment result from payment gateway. Status='SUCCESS' confirms booking, others mark as FAILED and release tickets.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Webhook processed"),
                    @ApiResponse(responseCode = "404", description = "Booking not found")
            }
    )
    public ResponseEntity<Map<String, String>> processPayment(@Valid @RequestBody PaymentWebhookRequest request) {
        bookingService.processPaymentWebhook(request);
        return ResponseEntity.ok(Map.of("message", "Webhook processed successfully"));
    }
}
