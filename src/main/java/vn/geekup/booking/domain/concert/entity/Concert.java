package vn.geekup.booking.domain.concert.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.geekup.booking.common.entity.BaseAuditEntity;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "concerts")
@Getter
@Setter
public class Concert extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConcertStatus status;

    @OneToMany(mappedBy = "concert")
    private List<TicketCategory> ticketCategories = new ArrayList<>();
}
