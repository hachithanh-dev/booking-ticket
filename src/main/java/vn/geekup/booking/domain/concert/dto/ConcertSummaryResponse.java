package vn.geekup.booking.domain.concert.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConcertSummaryResponse(
        UUID id,
        String name,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {
}
