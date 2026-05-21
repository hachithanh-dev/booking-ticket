package vn.geekup.booking.domain.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.common.entity.BaseAuditEntity;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.user.entity.User;
import vn.geekup.booking.domain.voucher.entity.Voucher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_booking_user_client_request", columnNames = {"user_id", "client_request_id"})
})
@Getter
@Setter
public class Booking extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "client_request_id", nullable = false, length = 100)
    private String clientRequestId;

    // Partial unique index in DB (WHERE status = 'PENDING').
    // JPA does not support partial indexes — uniqueness enforced by DB only.
    @Column(name = "business_fingerprint", nullable = false, length = 128)
    private String businessFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingStatus status;

    @Column(name = "subtotal_amount", nullable = false)
    private Long subtotalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItem> items = new ArrayList<>();
}
