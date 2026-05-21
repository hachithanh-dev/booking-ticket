package vn.geekup.booking.domain.voucher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.domain.booking.entity.Booking;
import vn.geekup.booking.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voucher_redemptions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_voucher_user", columnNames = {"voucher_id", "user_id"}),
        @UniqueConstraint(name = "uq_voucher_redemption_booking", columnNames = {"booking_id"})
})
@Getter
@Setter
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private LocalDateTime redeemedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.redeemedAt == null) {
            this.redeemedAt = LocalDateTime.now();
        }
    }
}
