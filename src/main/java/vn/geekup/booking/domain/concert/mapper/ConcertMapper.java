package vn.geekup.booking.domain.concert.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.geekup.booking.domain.concert.dto.ConcertDetailResponse;
import vn.geekup.booking.domain.concert.dto.ConcertSummaryResponse;
import vn.geekup.booking.domain.concert.dto.TicketCategoryResponse;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.ticket.entity.TicketCategory;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConcertMapper {

    @Mapping(target = "status", expression = "java(concert.getStatus().name())")
    ConcertSummaryResponse toSummaryResponse(Concert concert);

    @Mapping(target = "status", expression = "java(concert.getStatus().name())")
    @Mapping(target = "ticketCategories", source = "ticketCategories")
    ConcertDetailResponse toDetailResponse(Concert concert);

    @Mapping(target = "status", expression = "java(tc.getStatus().name())")
    TicketCategoryResponse toTicketCategoryResponse(TicketCategory tc);

    List<TicketCategoryResponse> toTicketCategoryResponses(List<TicketCategory> ticketCategories);
}
