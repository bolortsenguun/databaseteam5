package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LessonResultResponse(
        Long reservationId,
        String status,
        @JsonProperty("isDeducted") boolean isDeducted,
        int deductionCount,
        int remainingCount
) {
}