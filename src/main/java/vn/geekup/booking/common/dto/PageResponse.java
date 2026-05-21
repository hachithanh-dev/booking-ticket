package vn.geekup.booking.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int pageNo,
        int pageSize,
        long totalElements
) {
}
