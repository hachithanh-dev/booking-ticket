package vn.geekup.booking.domain.ticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.geekup.booking.domain.concert.dto.TicketCategoryResponse;
import vn.geekup.booking.domain.ticket.service.TicketCategoryCacheService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket category detail APIs")
public class TicketCategoryController {

    private final TicketCategoryCacheService ticketCategoryCacheService;

    @GetMapping("/{ticketCategoryId}")
    @Operation(
            summary = "Get ticket category detail",
            description = "Returns ticket category info including real-time availability. "
                    + "Uses cached static data merged with dynamic availableQuantity.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ticket category found"),
                    @ApiResponse(responseCode = "404", description = "Ticket category not found")
            }
    )
    public ResponseEntity<TicketCategoryResponse> getTicketCategory(
            @Parameter(description = "Ticket category UUID")
            @PathVariable UUID ticketCategoryId) {
        return ResponseEntity.ok(
                ticketCategoryCacheService.getTicketCategoryCached(ticketCategoryId));
    }
}
