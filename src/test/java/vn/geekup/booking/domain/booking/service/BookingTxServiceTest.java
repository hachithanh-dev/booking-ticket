package vn.geekup.booking.domain.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
import vn.geekup.booking.domain.voucher.entity.VoucherStatus;
import vn.geekup.booking.domain.voucher.repository.VoucherRedemptionRepository;
import vn.geekup.booking.domain.voucher.repository.VoucherRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingTxServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ConcertRepository concertRepository;
    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherRedemptionRepository voucherRedemptionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingTxService bookingTxService;

    // Common test data
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONCERT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TICKET_VIP_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01");
    private static final UUID TICKET_STD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02");
    private static final UUID VOUCHER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String CLIENT_REQUEST_ID = "test-order-001";
    private static final String FINGERPRINT = "sha256-test-fingerprint";

    private User testUser;
    private Concert testConcert;
    private TicketCategory vipTicket;
    private TicketCategory stdTicket;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("user1@test.com");
        testUser.setFullName("Test User 1");

        testConcert = new Concert();
        testConcert.setId(CONCERT_ID);
        testConcert.setName("Rock Festival 2026");
        testConcert.setStatus(ConcertStatus.PUBLISHED);

        vipTicket = new TicketCategory();
        vipTicket.setId(TICKET_VIP_ID);
        vipTicket.setName("VIP");
        vipTicket.setPriceAmount(2_000_000L);
        vipTicket.setTotalQuantity(100);
        vipTicket.setAvailableQuantity(100);

        stdTicket = new TicketCategory();
        stdTicket.setId(TICKET_STD_ID);
        stdTicket.setName("Standard");
        stdTicket.setPriceAmount(500_000L);
        stdTicket.setTotalQuantity(500);
        stdTicket.setAvailableQuantity(500);
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private ReserveBookingRequest buildRequest(UUID ticketId, int qty) {
        return new ReserveBookingRequest(CONCERT_ID,
                List.of(new ReserveBookingItemRequest(ticketId, qty)), null);
    }

    private ReserveBookingRequest buildRequestWithVoucher(UUID ticketId, int qty, String voucher) {
        return new ReserveBookingRequest(CONCERT_ID,
                List.of(new ReserveBookingItemRequest(ticketId, qty)), voucher);
    }

    private Voucher buildVoucher(DiscountType type, long value, Long maxDiscount) {
        Voucher v = new Voucher();
        v.setId(VOUCHER_ID);
        v.setCode("SUMMER2026");
        v.setDiscountType(type);
        v.setDiscountValue(value);
        v.setMaxDiscountAmount(maxDiscount);
        v.setMaxRedemptions(100);
        v.setUsedCount(0);
        v.setStartsAt(LocalDateTime.now().minusDays(30));
        v.setEndsAt(LocalDateTime.now().plusDays(30));
        v.setStatus(VoucherStatus.ACTIVE);
        return v;
    }

    private void stubHappyPath() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(concertRepository.findById(CONCERT_ID)).thenReturn(Optional.of(testConcert));
        when(ticketCategoryRepository.findById(TICKET_VIP_ID)).thenReturn(Optional.of(vipTicket));
        when(ticketCategoryRepository.decrementAvailableQuantity(TICKET_VIP_ID, 2)).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
    }

    // ── executeReservation ──────────────────────────────────────────────

    @Nested
    @DisplayName("executeReservation")
    class ExecuteReservation {

        @Test
        @DisplayName("Happy path — reserve 2 VIP tickets → PENDING booking with correct amounts")
        void happyPath() {
            stubHappyPath();
            ReserveBookingRequest req = buildRequest(TICKET_VIP_ID, 2);

            Booking result = bookingTxService.executeReservation(USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(result.getSubtotalAmount()).isEqualTo(4_000_000L);
            assertThat(result.getTotalAmount()).isEqualTo(4_000_000L);
            assertThat(result.getDiscountAmount()).isEqualTo(0L);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());

            verify(ticketCategoryRepository).decrementAvailableQuantity(TICKET_VIP_ID, 2);
            verify(eventPublisher).publishEvent(any(TicketQuantityChangedEvent.class));
        }

        @Test
        @DisplayName("User not found → ResourceNotFoundException")
        void userNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, buildRequest(TICKET_VIP_ID, 2)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Concert not found → ResourceNotFoundException")
        void concertNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(concertRepository.findById(CONCERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, buildRequest(TICKET_VIP_ID, 2)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Concert not found");
        }

        @Test
        @DisplayName("Concert not PUBLISHED → BusinessException")
        void concertNotPublished() {
            testConcert.setStatus(ConcertStatus.DRAFT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(concertRepository.findById(CONCERT_ID)).thenReturn(Optional.of(testConcert));

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, buildRequest(TICKET_VIP_ID, 2)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("Ticket sold out (decrement returns 0) → BusinessException")
        void ticketSoldOut() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(concertRepository.findById(CONCERT_ID)).thenReturn(Optional.of(testConcert));
            when(ticketCategoryRepository.findById(TICKET_VIP_ID)).thenReturn(Optional.of(vipTicket));
            when(ticketCategoryRepository.decrementAvailableQuantity(TICKET_VIP_ID, 2)).thenReturn(0);

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, buildRequest(TICKET_VIP_ID, 2)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("VIP");
        }

        @Test
        @DisplayName("With voucher PERCENTAGE — discount 10%, max 100k → correct calculation")
        void withVoucherPercentage() {
            // Stub only what's needed (Standard ticket, not VIP)
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(concertRepository.findById(CONCERT_ID)).thenReturn(Optional.of(testConcert));
            when(ticketCategoryRepository.findById(TICKET_STD_ID)).thenReturn(Optional.of(stdTicket));
            when(ticketCategoryRepository.decrementAvailableQuantity(TICKET_STD_ID, 3)).thenReturn(1);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                if (b.getId() == null) b.setId(UUID.randomUUID());
                return b;
            });

            Voucher voucher = buildVoucher(DiscountType.PERCENTAGE, 10L, 100_000L);
            when(voucherRepository.findByCode("SUMMER2026")).thenReturn(Optional.of(voucher));
            when(voucherRepository.incrementUsedCount(VOUCHER_ID)).thenReturn(1);
            when(voucherRedemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReserveBookingRequest req = buildRequestWithVoucher(TICKET_STD_ID, 3, "SUMMER2026");
            Booking result = bookingTxService.executeReservation(USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, req);

            // 3 × 500,000 = 1,500,000. 10% = 150,000 → capped at 100,000
            assertThat(result.getSubtotalAmount()).isEqualTo(1_500_000L);
            assertThat(result.getDiscountAmount()).isEqualTo(100_000L);
            assertThat(result.getTotalAmount()).isEqualTo(1_400_000L);
            verify(voucherRepository).incrementUsedCount(VOUCHER_ID);
            verify(voucherRedemptionRepository).save(any());
        }

        @Test
        @DisplayName("With voucher FIXED_AMOUNT → correct discount")
        void withVoucherFixed() {
            stubHappyPath();
            Voucher voucher = buildVoucher(DiscountType.FIXED_AMOUNT, 500_000L, null);
            when(voucherRepository.findByCode("FIXED500K")).thenReturn(Optional.of(voucher));
            when(voucherRepository.incrementUsedCount(VOUCHER_ID)).thenReturn(1);
            when(voucherRedemptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReserveBookingRequest req = buildRequestWithVoucher(TICKET_VIP_ID, 2, "FIXED500K");
            Booking result = bookingTxService.executeReservation(USER_ID, CLIENT_REQUEST_ID, FINGERPRINT, req);

            // 2 × 2,000,000 = 4,000,000 — discount 500,000
            assertThat(result.getSubtotalAmount()).isEqualTo(4_000_000L);
            assertThat(result.getDiscountAmount()).isEqualTo(500_000L);
            assertThat(result.getTotalAmount()).isEqualTo(3_500_000L);
        }

        @Test
        @DisplayName("Voucher not found → ResourceNotFoundException")
        void voucherNotFound() {
            stubHappyPath();
            when(voucherRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT,
                    buildRequestWithVoucher(TICKET_VIP_ID, 2, "INVALID")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Voucher not found");
        }

        @Test
        @DisplayName("Voucher not ACTIVE → BusinessException")
        void voucherNotActive() {
            stubHappyPath();
            Voucher voucher = buildVoucher(DiscountType.PERCENTAGE, 10L, null);
            voucher.setStatus(VoucherStatus.INACTIVE);
            when(voucherRepository.findByCode("INACTIVE")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT,
                    buildRequestWithVoucher(TICKET_VIP_ID, 2, "INACTIVE")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Voucher expired → BusinessException")
        void voucherExpired() {
            stubHappyPath();
            Voucher voucher = buildVoucher(DiscountType.PERCENTAGE, 10L, null);
            voucher.setStartsAt(LocalDateTime.now().minusDays(60));
            voucher.setEndsAt(LocalDateTime.now().minusDays(1)); // expired yesterday
            when(voucherRepository.findByCode("EXPIRED")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT,
                    buildRequestWithVoucher(TICKET_VIP_ID, 2, "EXPIRED")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Voucher max redemptions reached (incrementUsedCount returns 0) → BusinessException")
        void voucherMaxRedemptions() {
            stubHappyPath();
            Voucher voucher = buildVoucher(DiscountType.PERCENTAGE, 10L, null);
            when(voucherRepository.findByCode("MAXED")).thenReturn(Optional.of(voucher));
            when(voucherRepository.incrementUsedCount(VOUCHER_ID)).thenReturn(0); // atomically failed

            assertThatThrownBy(() -> bookingTxService.executeReservation(
                    USER_ID, CLIENT_REQUEST_ID, FINGERPRINT,
                    buildRequestWithVoucher(TICKET_VIP_ID, 2, "MAXED")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("hết lượt");
        }
    }

    // ── processPayment ─────────────────────────────────────────────────

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        private Booking pendingBooking;

        @BeforeEach
        void setUp() {
            pendingBooking = new Booking();
            pendingBooking.setId(UUID.randomUUID());
            pendingBooking.setStatus(BookingStatus.PENDING);
            pendingBooking.setItems(new ArrayList<>());

            BookingItem item = new BookingItem();
            item.setTicketCategory(vipTicket);
            item.setQuantity(2);
            pendingBooking.getItems().add(item);
        }

        @Test
        @DisplayName("SUCCESS → status = CONFIRMED")
        void paymentSuccess() {
            when(bookingRepository.findByIdForUpdate(pendingBooking.getId()))
                    .thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking result = bookingTxService.processPayment(pendingBooking.getId(), "SUCCESS");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(ticketCategoryRepository, never()).incrementAvailableQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("FAILED → status = FAILED + tickets released")
        void paymentFailed() {
            when(bookingRepository.findByIdForUpdate(pendingBooking.getId()))
                    .thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking result = bookingTxService.processPayment(pendingBooking.getId(), "FAILED");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.FAILED);
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_VIP_ID, 2);
            verify(eventPublisher).publishEvent(any(TicketQuantityChangedEvent.class));
        }

        @Test
        @DisplayName("Already processed (CONFIRMED) → returns existing booking (idempotent)")
        void alreadyProcessed() {
            pendingBooking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findByIdForUpdate(pendingBooking.getId()))
                    .thenReturn(Optional.of(pendingBooking));

            Booking result = bookingTxService.processPayment(pendingBooking.getId(), "SUCCESS");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Booking not found → ResourceNotFoundException")
        void bookingNotFound() {
            UUID randomId = UUID.randomUUID();
            when(bookingRepository.findByIdForUpdate(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingTxService.processPayment(randomId, "SUCCESS"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── cancelBooking ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("Cancel PENDING → CANCELLED + tickets released")
        void cancelPending() {
            Booking booking = new Booking();
            booking.setId(UUID.randomUUID());
            booking.setStatus(BookingStatus.PENDING);
            booking.setItems(new ArrayList<>());
            BookingItem item = new BookingItem();
            item.setTicketCategory(vipTicket);
            item.setQuantity(1);
            booking.getItems().add(item);

            when(bookingRepository.findByIdForUpdate(booking.getId()))
                    .thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking result = bookingTxService.cancelBooking(booking.getId(), "User requested");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_VIP_ID, 1);
        }

        @Test
        @DisplayName("Cancel CONFIRMED → CANCELLED + tickets released")
        void cancelConfirmed() {
            Booking booking = new Booking();
            booking.setId(UUID.randomUUID());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setItems(new ArrayList<>());
            BookingItem item = new BookingItem();
            item.setTicketCategory(stdTicket);
            item.setQuantity(3);
            booking.getItems().add(item);

            when(bookingRepository.findByIdForUpdate(booking.getId()))
                    .thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Booking result = bookingTxService.cancelBooking(booking.getId(), "Admin cancel");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_STD_ID, 3);
        }

        @Test
        @DisplayName("Cancel EXPIRED → BusinessException")
        void cancelExpired() {
            Booking booking = new Booking();
            booking.setId(UUID.randomUUID());
            booking.setStatus(BookingStatus.EXPIRED);

            when(bookingRepository.findByIdForUpdate(booking.getId()))
                    .thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingTxService.cancelBooking(booking.getId(), "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EXPIRED");
        }
    }

    // ── batchExpireBookings ────────────────────────────────────────────

    @Nested
    @DisplayName("batchExpireBookings")
    class BatchExpire {

        @Test
        @DisplayName("Batch expire — aggregates ticket quantities correctly")
        void batchExpireAggregates() {
            // 2 bookings: booking1 has 2 VIP, booking2 has 1 VIP + 3 Standard
            Booking b1 = new Booking();
            b1.setId(UUID.randomUUID());
            b1.setStatus(BookingStatus.PENDING);
            b1.setItems(new ArrayList<>());
            BookingItem b1i = new BookingItem();
            b1i.setTicketCategory(vipTicket);
            b1i.setQuantity(2);
            b1.getItems().add(b1i);

            Booking b2 = new Booking();
            b2.setId(UUID.randomUUID());
            b2.setStatus(BookingStatus.PENDING);
            b2.setItems(new ArrayList<>());
            BookingItem b2i1 = new BookingItem();
            b2i1.setTicketCategory(vipTicket);
            b2i1.setQuantity(1);
            BookingItem b2i2 = new BookingItem();
            b2i2.setTicketCategory(stdTicket);
            b2i2.setQuantity(3);
            b2.getItems().add(b2i1);
            b2.getItems().add(b2i2);

            List<UUID> ids = List.of(b1.getId(), b2.getId());
            when(bookingRepository.findAllPendingByIdsForUpdate(ids))
                    .thenReturn(List.of(b1, b2));

            int expired = bookingTxService.batchExpireBookings(ids);

            assertThat(expired).isEqualTo(2);
            assertThat(b1.getStatus()).isEqualTo(BookingStatus.EXPIRED);
            assertThat(b2.getStatus()).isEqualTo(BookingStatus.EXPIRED);

            // VIP: 2 + 1 = 3 total
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_VIP_ID, 3);
            // Standard: 3
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_STD_ID, 3);

            verify(bookingRepository).saveAll(List.of(b1, b2));
        }

        @Test
        @DisplayName("Empty list (all already processed) → returns 0")
        void emptyList() {
            List<UUID> ids = List.of(UUID.randomUUID());
            when(bookingRepository.findAllPendingByIdsForUpdate(ids))
                    .thenReturn(List.of()); // all already processed

            int expired = bookingTxService.batchExpireBookings(ids);

            assertThat(expired).isZero();
            verify(ticketCategoryRepository, never()).incrementAvailableQuantity(any(), anyInt());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Publishes single event with all unique ticketCategoryIds")
        void publishesSingleEvent() {
            Booking b1 = new Booking();
            b1.setId(UUID.randomUUID());
            b1.setStatus(BookingStatus.PENDING);
            b1.setItems(new ArrayList<>());
            BookingItem item = new BookingItem();
            item.setTicketCategory(vipTicket);
            item.setQuantity(1);
            b1.getItems().add(item);

            when(bookingRepository.findAllPendingByIdsForUpdate(any()))
                    .thenReturn(List.of(b1));

            bookingTxService.batchExpireBookings(List.of(b1.getId()));

            ArgumentCaptor<TicketQuantityChangedEvent> captor =
                    ArgumentCaptor.forClass(TicketQuantityChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            TicketQuantityChangedEvent event = captor.getValue();
            assertThat(event.ticketCategoryIds()).containsExactly(TICKET_VIP_ID);
        }
    }

    // ── releaseTickets ─────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseTickets")
    class ReleaseTickets {

        @Test
        @DisplayName("Restores ticket quantities for all items + publishes event")
        void restoresTickets() {
            Booking booking = new Booking();
            booking.setItems(new ArrayList<>());

            BookingItem item1 = new BookingItem();
            item1.setTicketCategory(vipTicket);
            item1.setQuantity(2);
            BookingItem item2 = new BookingItem();
            item2.setTicketCategory(stdTicket);
            item2.setQuantity(5);
            booking.getItems().add(item1);
            booking.getItems().add(item2);

            bookingTxService.releaseTickets(booking);

            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_VIP_ID, 2);
            verify(ticketCategoryRepository).incrementAvailableQuantity(TICKET_STD_ID, 5);
            verify(eventPublisher).publishEvent(any(TicketQuantityChangedEvent.class));
        }

        @Test
        @DisplayName("Does NOT restore voucher (business rule)")
        void doesNotRestoreVoucher() {
            Booking booking = new Booking();
            booking.setItems(new ArrayList<>());
            BookingItem item = new BookingItem();
            item.setTicketCategory(vipTicket);
            item.setQuantity(1);
            booking.getItems().add(item);

            bookingTxService.releaseTickets(booking);

            // Verify voucher repo methods are NEVER called
            verifyNoInteractions(voucherRepository);
            verifyNoInteractions(voucherRedemptionRepository);
        }
    }
}
