package com.fatenexx.backend.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminAttendanceListResponse(
        LocalDate date,
        int totalCount,
        List<AdminAttendanceItemResponse> attendances
) {
}