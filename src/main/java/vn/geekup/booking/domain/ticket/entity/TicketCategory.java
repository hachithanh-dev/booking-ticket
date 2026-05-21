package vn.geekup.booking.domain.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.common.entity.BaseAuditEntity;
import vn.geekup.booking.domain.concert.entity.Concert;

import java.util.UUID;

@Entity
@Table(name = "ticket_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ticket_category_name_per_concert", columnNames = {"concert_id", "name"})
})
@Getter
@Setter
public class TicketCategory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_amount", nullable = false)
    private Long priceAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketCategoryStatus status;
}
