package vn.geekup.booking.domain.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.geekup.booking.domain.booking.service.BookingService;
import vn.geekup.booking.domain.booking.service.BookingTxService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingService bookingService;
    private final BookingTxService bookingTxService;

    private static final int CHUNK_SIZE = 20;

    @Scheduled(cron = "0 * * * * *")
    public void scanAndExpireBookings() {
        List<UUID> expiredIds = bookingService.findExpiredPendingBookingIds();
        if (expiredIds.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING bookings, processing in chunks of {}...",
                expiredIds.size(), CHUNK_SIZE);

        int totalExpired = 0;

        // Process in chunks to reduce lock contention per transaction
        for (int i = 0; i < expiredIds.size(); i += CHUNK_SIZE) {
            List<UUID> chunk = expiredIds.subList(i,
                    Math.min(i + CHUNK_SIZE, expiredIds.size()));
            try {
                totalExpired += bookingTxService.batchExpireBookings(chunk);
            } catch (Exception e) {
                log.error("Failed to expire chunk [{}-{}]: {}",
                        i, i + chunk.size() - 1, e.getMessage());
            }
        }

        log.info("Batch expire completed: {}/{} bookings expired", totalExpired, expiredIds.size());
    }
}
