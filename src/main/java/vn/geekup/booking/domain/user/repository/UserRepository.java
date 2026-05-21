package vn.geekup.booking.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.geekup.booking.domain.user.entity.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
