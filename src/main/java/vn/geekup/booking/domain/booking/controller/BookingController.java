package vn.geekup.booking.domain.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.geekup.booking.config.AuthInterceptor;
import vn.geekup.booking.domain.booking.dto.BookingDetailResponse;
import vn.geekup.booking.domain.booking.dto.BookingResponse;
import vn.geekup.booking.domain.booking.dto.ReserveBookingRequest;
import vn.geekup.booking.domain.booking.service.BookingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Customer ticket booking APIs")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/reserve")
    @Operation(
            summary = "Reserve tickets",
            description = "Creates a PENDING booking with 15-minute expiry. Requires X-User-Id and X-Idempotency-Key headers. "
                    + "Supports optional voucher code for discounts.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Booking created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error or insufficient tickets"),
                    @ApiResponse(responseCode = "404", description = "Concert or ticket category not found"),
                    @ApiResponse(responseCode = "409", description = "Duplicate booking (idempotency conflict)")
            }
    )
    public ResponseEntity<BookingResponse> reserveTickets(
            @Valid @RequestBody ReserveBookingRequest request,
            @Parameter(description = "Idempotency key for duplicate prevention") @RequestHeader(value = "X-Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTR);
        BookingResponse response = bookingService.reserveTickets(userId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Get booking detail",
            description = "Returns booking detail including items. Only accessible by the booking owner (via X-User-Id header).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking found"),
                    @ApiResponse(responseCode = "404", description = "Booking not found or not owned by user")
            }
    )
    public ResponseEntity<BookingDetailResponse> getBookingDetail(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTR);
        return ResponseEntity.ok(bookingService.getBookingDetail(userId, bookingId));
    }
}
