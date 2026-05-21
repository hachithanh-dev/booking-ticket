package vn.geekup.booking.domain.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.geekup.booking.domain.booking.dto.AdminBookingResponse;
import vn.geekup.booking.domain.booking.dto.BookingDetailResponse;
import vn.geekup.booking.domain.booking.dto.BookingItemResponse;
import vn.geekup.booking.domain.booking.dto.BookingResponse;
import vn.geekup.booking.domain.booking.entity.Booking;
import vn.geekup.booking.domain.booking.entity.BookingItem;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toBookingResponse(Booking booking);

    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    @Mapping(target = "items", source = "items")
    BookingDetailResponse toDetailResponse(Booking booking);

    @Mapping(target = "ticketCategoryName", source = "ticketCategory.name")
    BookingItemResponse toItemResponse(BookingItem item);

    List<BookingItemResponse> toItemResponses(List<BookingItem> items);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "concertId", source = "concert.id")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    AdminBookingResponse toAdminResponse(Booking booking);
}
