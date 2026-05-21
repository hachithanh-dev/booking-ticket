package vn.geekup.booking.domain.concert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.geekup.booking.common.dto.PageResponse;
import vn.geekup.booking.domain.concert.dto.ConcertDetailResponse;
import vn.geekup.booking.domain.concert.dto.ConcertSummaryResponse;
import vn.geekup.booking.domain.concert.entity.Concert;
import vn.geekup.booking.domain.concert.entity.ConcertStatus;
import vn.geekup.booking.domain.concert.mapper.ConcertMapper;
import vn.geekup.booking.domain.concert.repository.ConcertRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertMapper concertMapper;
    private final ConcertCacheService concertCacheService;

    @Transactional(readOnly = true)
    public PageResponse<ConcertSummaryResponse> findPublishedConcerts(Pageable pageable) {
        Page<Concert> page = concertRepository.findByStatus(ConcertStatus.PUBLISHED, pageable);
        var content = page.getContent().stream()
                .map(concertMapper::toSummaryResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public ConcertDetailResponse getConcertDetail(UUID concertId) {
        return concertCacheService.getConcertDetailCached(concertId);
    }
}
