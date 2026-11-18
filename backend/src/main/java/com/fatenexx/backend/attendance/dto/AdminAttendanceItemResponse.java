package com.fatenexx.backend.attendance.dto;

import java.time.LocalDateTime;

public record AdminAttendanceItemResponse(
        Long attendanceId,
        Long userId,
        String userName,
        String phone,
        LocalDateTime checkedInAt,
        String entryType,
        Long reservationId
) {
}