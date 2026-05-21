package vn.geekup.booking.domain.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.common.entity.BaseCreatableEntity;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;

import java.util.UUID;

@Entity
@Table(name = "booking_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_booking_item_category", columnNames = {"booking_id", "ticket_category_id"})
})
@Getter
@Setter
public class BookingItem extends BaseCreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory ticketCategory;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_amount", nullable = false)
    private Long unitPriceAmount;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;
}
