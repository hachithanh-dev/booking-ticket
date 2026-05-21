package vn.geekup.booking.domain.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;

import java.util.UUID;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {

    @Modifying
    @Query("UPDATE TicketCategory tc SET tc.availableQuantity = tc.availableQuantity - :qty " +
           "WHERE tc.id = :id AND tc.availableQuantity >= :qty")
    int decrementAvailableQuantity(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE TicketCategory tc SET tc.availableQuantity = tc.availableQuantity + :qty " +
           "WHERE tc.id = :id")
    int incrementAvailableQuantity(@Param("id") UUID id, @Param("qty") int qty);
}
