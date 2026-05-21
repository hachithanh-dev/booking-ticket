package vn.geekup.booking.domain.voucher.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.common.entity.BaseAuditEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
public class Voucher extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 50)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "max_redemptions", nullable = false)
    private Integer maxRedemptions;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VoucherStatus status;
}
