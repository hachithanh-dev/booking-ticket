package vn.geekup.booking.domain.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.geekup.booking.domain.booking.entity.BookingItem;

import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
}
