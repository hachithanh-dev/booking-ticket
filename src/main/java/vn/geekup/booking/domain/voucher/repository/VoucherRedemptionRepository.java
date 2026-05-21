package vn.geekup.booking.domain.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.geekup.booking.domain.voucher.entity.VoucherRedemption;

import java.util.UUID;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {

    @Modifying
    @Query("DELETE FROM VoucherRedemption vr WHERE vr.booking.id = :bookingId")
    void deleteByBookingId(@Param("bookingId") UUID bookingId);
}
