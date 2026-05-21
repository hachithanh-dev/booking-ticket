package vn.geekup.booking.domain.concert.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConcertDetailResponse(
        UUID id,
        String name,
        String description,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        List<TicketCategoryResponse> ticketCategories
) {
}
