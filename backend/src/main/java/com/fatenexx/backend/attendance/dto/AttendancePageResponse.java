package com.fatenexx.backend.attendance.dto;

import java.util.List;

public record AttendancePageResponse(
        List<AttendanceItemResponse> attendances,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}