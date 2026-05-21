package vn.geekup.booking.domain.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.geekup.booking.common.dto.PageResponse;
import vn.geekup.booking.common.exception.BusinessException;
import vn.geekup.booking.common.exception.ResourceNotFoundException;
import vn.geekup.booking.domain.booking.dto.*;
import vn.geekup.booking.domain.booking.entity.Booking;
import vn.geekup.booking.domain.booking.entity.BookingStatus;
import vn.geekup.booking.domain.booking.mapper.BookingMapper;
import vn.geekup.booking.domain.booking.repository.BookingRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingTxService bookingTxService;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingLockService bookingLockService;

    /**
     * Reserve tickets — 3-Zone Pattern + SETNX idempotency.
     * Zone 0: Redis SETNX — block duplicate requests immediately
     * Zone 1: Build fingerprint (pre-tx)
     * Zone 2: Execute reservation (tx via BookingTxService)
     * Zone 3: Log (post-tx)
     */
    public BookingResponse reserveTickets(UUID userId, String idempotencyKey, ReserveBookingRequest request) {
        if (!bookingLockService.acquireIdempotencyLock(userId, idempotencyKey)) {
            throw new BusinessException(
                    "Duplicate request. Please wait for the previous request to complete.");
        }

        try {
            String fingerprint = buildFingerprint(userId, request);
            Booking booking = bookingTxService.executeReservation(
                    userId, idempotencyKey, fingerprint, request);

            log.info("Booking {} created for user {}", booking.getId(), userId);
            return bookingMapper.toBookingResponse(booking);
        } catch (Exception e) {
            bookingLockService.releaseIdempotencyLock(userId, idempotencyKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        return bookingMapper.toDetailResponse(booking);
    }

    public void processPaymentWebhook(PaymentWebhookRequest request) {
        bookingTxService.processPayment(request.bookingId(), request.status());
        log.info("Payment webhook processed: booking={}, txn={}, status={}",
                request.bookingId(), request.transactionId(), request.status());
    }

    public void expireBooking(UUID bookingId) {
        bookingTxService.expireBooking(bookingId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookingResponse> findBookingsForAdmin(UUID concertId, String status, Pageable pageable) {
        BookingStatus bookingStatus = parseBookingStatus(status);
        Page<Booking> page = bookingRepository.findByFilters(concertId, bookingStatus, pageable);
        var content = page.getContent().stream()
                .map(bookingMapper::toAdminResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public BookingResponse cancelBookingByAdmin(UUID bookingId, UpdateBookingStatusRequest request) {
        Booking booking = bookingTxService.cancelBooking(bookingId, request.reason());
        return bookingMapper.toBookingResponse(booking);
    }

    public List<UUID> findExpiredPendingBookingIds() {
        return bookingRepository.findExpiredPendingBookingIds(LocalDateTime.now());
    }

    private BookingStatus parseBookingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status: " + status);
        }
    }

    private String buildFingerprint(UUID userId, ReserveBookingRequest request) {
        String sorted = request.items().stream()
                .map(i -> i.ticketCategoryId() + ":" + i.quantity())
                .sorted()
                .collect(Collectors.joining(","));
        String raw = userId + ":" + request.concertId() + ":" + sorted;
        return sha256(raw);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
