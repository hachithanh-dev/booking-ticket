package vn.geekup.booking.domain.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.geekup.booking.common.dto.PageResponse;
import vn.geekup.booking.domain.booking.dto.AdminBookingResponse;
import vn.geekup.booking.domain.booking.dto.BookingResponse;
import vn.geekup.booking.domain.booking.dto.UpdateBookingStatusRequest;
import vn.geekup.booking.domain.booking.service.BookingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Booking", description = "Admin booking management APIs. Requires X-Role: ADMIN header.")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(
            summary = "List all bookings",
            description = "Returns paginated bookings with optional filters by concertId and status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bookings retrieved"),
                    @ApiResponse(responseCode = "403", description = "Not an admin")
            }
    )
    public ResponseEntity<PageResponse<AdminBookingResponse>> listBookings(
            @Parameter(description = "Filter by concert UUID") @RequestParam(required = false) UUID concertId,
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED, EXPIRED, FAILED") @RequestParam(required = false) String status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.findBookingsForAdmin(concertId, status, PageRequest.of(page, size)));
    }

    @PatchMapping("/{bookingId}/status")
    @Operation(
            summary = "Cancel booking",
            description = "Admin cancels a booking (PENDING or CONFIRMED). Releases tickets and voucher.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking cancelled"),
                    @ApiResponse(responseCode = "400", description = "Invalid status transition"),
                    @ApiResponse(responseCode = "403", description = "Not an admin"),
                    @ApiResponse(responseCode = "404", description = "Booking not found")
            }
    )
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return ResponseEntity.ok(bookingService.cancelBookingByAdmin(bookingId, request));
    }
}
