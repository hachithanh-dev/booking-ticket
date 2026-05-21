package vn.geekup.booking.domain.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.geekup.booking.common.exception.BusinessException;
import vn.geekup.booking.common.exception.ResourceNotFoundException;
import vn.geekup.booking.domain.booking.dto.ReserveBookingItemRequest;
import vn.geekup.booking.domain.booking.dto.ReserveBookingRequest;
import vn.geekup.booking.domain.booking.entity.Booking;
import vn.geekup.booking.domain.booking.entity.BookingItem;
import vn.geekup.booking.domain.booking.entity.BookingStatus;
import vn.geekup.booking.domain.booking.event.TicketQuantityChangedEvent;
import vn.geekup.booking.domain.booking.repository.BookingRepository;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.concert.entity.ConcertStatus;
import vn.geekup.booking.domain.concert.repository.ConcertRepository;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;
import vn.geekup.booking.domain.ticket.repository.TicketCategoryRepository;
import vn.geekup.booking.domain.user.entity.User;
import vn.geekup.booking.domain.user.repository.UserRepository;
import vn.geekup.booking.domain.voucher.entity.DiscountType;
import vn.geekup.booking.domain.voucher.entity.Voucher;
import vn.geekup.booking.domain.voucher.entity.VoucherRedemption;
import vn.geekup.booking.domain.voucher.entity.VoucherStatus;
import vn.geekup.booking.domain.voucher.repository.VoucherRedemptionRepository;
import vn.geekup.booking.domain.voucher.repository.VoucherRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingTxService {

    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Booking executeReservation(UUID userId, String clientRequestId, String businessFingerprint,
                                      ReserveBookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Concert concert = concertRepository.findById(request.concertId())
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + request.concertId()));
        if (concert.getStatus() != ConcertStatus.PUBLISHED) {
            throw new BusinessException("Concert is not available for booking");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setConcert(concert);
        booking.setClientRequestId(clientRequestId);
        booking.setBusinessFingerprint(businessFingerprint);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCurrency("VND");
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        long subtotal = 0;
        for (ReserveBookingItemRequest itemReq : request.items()) {
            subtotal += reserveItem(booking, itemReq);
        }

        booking.setSubtotalAmount(subtotal);
        booking.setDiscountAmount(0L);
        booking.setTotalAmount(subtotal);

        Booking saved = bookingRepository.save(booking);

        long discount = 0;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            discount = applyVoucher(request.voucherCode(), user, saved, subtotal);
            saved.setDiscountAmount(discount);
            saved.setTotalAmount(subtotal - discount);
            saved = bookingRepository.save(saved);
        }

        List<UUID> ticketIds = saved.getItems().stream()
                .map(item -> item.getTicketCategory().getId()).toList();
        eventPublisher.publishEvent(new TicketQuantityChangedEvent(ticketIds));

        return saved;
    }

    private long reserveItem(Booking booking, ReserveBookingItemRequest itemReq) {
        TicketCategory tc = ticketCategoryRepository.findById(itemReq.ticketCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket category not found: " + itemReq.ticketCategoryId()));

        int updated = ticketCategoryRepository.decrementAvailableQuantity(tc.getId(), itemReq.quantity());
        if (updated == 0) {
            throw new BusinessException("Vé " + tc.getName() + " đã bán hết hoặc không đủ số lượng.");
        }

        BookingItem item = new BookingItem();
        item.setBooking(booking);
        item.setTicketCategory(tc);
        item.setQuantity(itemReq.quantity());
        item.setUnitPriceAmount(tc.getPriceAmount());
        item.setTotalAmount(tc.getPriceAmount() * itemReq.quantity());
        booking.getItems().add(item);
        return item.getTotalAmount();
    }

    private long applyVoucher(String voucherCode, User user, Booking booking, long subtotal) {
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + voucherCode));

        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new BusinessException("Voucher is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartsAt()) || now.isAfter(voucher.getEndsAt())) {
            throw new BusinessException("Voucher has expired or not yet started");
        }

        int updated = voucherRepository.incrementUsedCount(voucher.getId());
        if (updated == 0) {
            throw new BusinessException("Voucher đã hết lượt sử dụng.");
        }

        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setUser(user);
        redemption.setBooking(booking);
        voucherRedemptionRepository.save(redemption);

        booking.setVoucher(voucher);

        return calculateDiscount(voucher, subtotal);
    }

    private long calculateDiscount(Voucher voucher, long subtotal) {
        long discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal * voucher.getDiscountValue() / 100;
            if (voucher.getMaxDiscountAmount() != null) {
                discount = Math.min(discount, voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        return Math.min(discount, subtotal); // discount cannot exceed subtotal
    }

    /**
     * Release tickets only — voucher NOT restored.
     * <p>
     * Business rule: mỗi user chỉ được sử dụng voucher 1 lần.
     * Nếu không thanh toán (expire/fail/cancel), voucher coi như đã dùng.
     * voucher_redemptions record giữ lại → chặn tái sử dụng.
     * <p>
     * MANDATORY: phải gọi từ bên trong @Transactional context.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseTickets(Booking booking) {
        // Restore ticket quantities only
        for (BookingItem item : booking.getItems()) {
            ticketCategoryRepository.incrementAvailableQuantity(
                    item.getTicketCategory().getId(), item.getQuantity());
        }

        // Publish event — fires AFTER_COMMIT to invalidate ticket dynamic cache
        List<UUID> ticketIds = booking.getItems().stream()
                .map(item -> item.getTicketCategory().getId()).toList();
        eventPublisher.publishEvent(new TicketQuantityChangedEvent(ticketIds));
    }

    @Transactional
    public Booking processPayment(UUID bookingId, String status) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Booking {} is not PENDING, current status: {}", bookingId, booking.getStatus());
            return booking; // Idempotent — already processed
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Booking {} confirmed", bookingId);
        } else {
            booking.setStatus(BookingStatus.FAILED);
            releaseTickets(booking);
            log.info("Booking {} failed, tickets released", bookingId);
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public void expireBooking(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            return; // Already processed
        }

        booking.setStatus(BookingStatus.EXPIRED);
        releaseTickets(booking);
        bookingRepository.save(booking);
        log.info("Booking {} expired, tickets released", bookingId);
    }

    @Transactional
    public Booking cancelBooking(UUID bookingId, String reason) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Cannot cancel booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        releaseTickets(booking);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled. Reason: {}", bookingId, reason);
        return booking;
    }

    /**
     * Batch expire bookings — 1 transaction per chunk.
     * <p>
     * Optimization: 1 SELECT FETCH JOIN + aggregate ticket qty + batch UPDATE.
     * Voucher NOT restored (business rule).
     */
    @Transactional
    public int batchExpireBookings(List<UUID> bookingIds) {
        List<Booking> pendingBookings = bookingRepository
                .findAllPendingByIdsForUpdate(bookingIds);

        if (pendingBookings.isEmpty()) {
            return 0;
        }

        Map<UUID, Integer> ticketQtyMap = new HashMap<>();
        for (Booking booking : pendingBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            for (BookingItem item : booking.getItems()) {
                ticketQtyMap.merge(
                        item.getTicketCategory().getId(),
                        item.getQuantity(),
                        Integer::sum);
            }
        }

        for (var entry : ticketQtyMap.entrySet()) {
            ticketCategoryRepository.incrementAvailableQuantity(
                    entry.getKey(), entry.getValue());
        }

        bookingRepository.saveAll(pendingBookings);

        List<UUID> ticketCategoryIds = new ArrayList<>(ticketQtyMap.keySet());
        eventPublisher.publishEvent(new TicketQuantityChangedEvent(ticketCategoryIds));

        log.info("Batch expired {} bookings, restored {} ticket categories",
                pendingBookings.size(), ticketQtyMap.size());

        return pendingBookings.size();
    }
}
