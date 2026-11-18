package com.fatenexx.backend.attendance.dto;

import java.time.LocalDateTime;

public record AttendanceItemResponse(
        Long attendanceId,
        LocalDateTime checkedInAt,
        String entryType,
        Long reservationId
) {
}