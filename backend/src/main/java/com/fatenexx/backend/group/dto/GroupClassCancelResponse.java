package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record GroupClassCancelResponse(
        Long classId,
        String status,
        String cancelReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime canceledAt,
        int canceledReservations
) {
}