package vn.geekup.booking.domain.booking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.geekup.booking.domain.booking.entity.Booking;
import vn.geekup.booking.domain.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.items bi " +
           "LEFT JOIN FETCH bi.ticketCategory " +
           "WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT b.id FROM Booking b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<UUID> findExpiredPendingBookingIds(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"user", "concert"})
    @Query("SELECT b FROM Booking b WHERE (:concertId IS NULL OR b.concert.id = :concertId) " +
           "AND (:status IS NULL OR b.status = :status)")
    Page<Booking> findByFilters(@Param("concertId") UUID concertId,
                                @Param("status") BookingStatus status,
                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.items bi " +
           "LEFT JOIN FETCH bi.ticketCategory " +
           "WHERE b.id IN :ids AND b.status = 'PENDING'")
    List<Booking> findAllPendingByIdsForUpdate(@Param("ids") List<UUID> ids);
}
