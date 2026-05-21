package vn.geekup.booking.domain.concert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.geekup.booking.common.dto.PageResponse;
import vn.geekup.booking.domain.concert.dto.ConcertDetailResponse;
import vn.geekup.booking.domain.concert.dto.ConcertSummaryResponse;
import vn.geekup.booking.domain.concert.service.ConcertService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
@Tag(name = "Concert", description = "Public concert browsing APIs")
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    @Operation(
            summary = "List published concerts",
            description = "Returns a paginated list of concerts with status PUBLISHED",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Concerts retrieved successfully")
            }
    )
    public ResponseEntity<PageResponse<ConcertSummaryResponse>> listConcerts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(concertService.findPublishedConcerts(PageRequest.of(page, size)));
    }

    @GetMapping("/{concertId}")
    @Operation(
            summary = "Get concert detail",
            description = "Returns concert information including all ticket categories",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Concert found"),
                    @ApiResponse(responseCode = "404", description = "Concert not found")
            }
    )
    public ResponseEntity<ConcertDetailResponse> getConcertDetail(
            @Parameter(description = "Concert UUID") @PathVariable UUID concertId) {
        return ResponseEntity.ok(concertService.getConcertDetail(concertId));
    }
}
