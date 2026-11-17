package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

public record LessonParticipantResponse(
        Long reservationId,
        Long userId,
        String name,
        String phone,
        String status,
        @JsonProperty("isDeducted") boolean isDeducted
) {
    public static LessonParticipantResponse from(PtReservation r) {
        return new LessonParticipantResponse(r.getId(), r.getUser().getId(), r.getUser().getName(),
                r.getUser().getPhone(), r.getStatus().getLabel(), r.isDeducted());
    }
}