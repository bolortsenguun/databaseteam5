package com.fatenexx.backend.attendance.dto;

import java.util.List;

// 관리자 출석 목록 (page는 1부터)
public record AdminAttendanceListResponse(
        List<AdminAttendanceItemResponse> attendances,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
