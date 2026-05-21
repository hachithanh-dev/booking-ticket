package vn.geekup.booking.domain.concert.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.concert.entity.ConcertStatus;

import java.util.Optional;
import java.util.UUID;

public interface ConcertRepository extends JpaRepository<Concert, UUID> {

    Page<Concert> findByStatus(ConcertStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"ticketCategories"})
    @Query("SELECT c FROM Concert c WHERE c.id = :id")
    Optional<Concert> findDetailById(@Param("id") UUID id);
}
