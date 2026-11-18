package com.fatenexx.backend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record AdminAttendanceItemResponse(
        Long attendanceId,
        Long userId,
        String userName,
        String phone,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt,
        String entryType,
        Long reservationId
) {
}