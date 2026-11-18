package com.fatenexx.backend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record AttendanceItemResponse(
        Long attendanceId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt,
        String entryType,
        Long reservationId
) {
}