package vn.geekup.booking.domain.booking.dto;

public record BookingItemResponse(
        String ticketCategoryName,
        int quantity,
        long unitPriceAmount,
        long totalAmount
) {
}
