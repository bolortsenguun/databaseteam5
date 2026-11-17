package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDateTime;

public record ReservationCancelResponse(
        Long reservationId,
        String status,
        String canceledBy,
        @JsonProperty("isDeducted") boolean isDeducted,
        int deductionCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime canceledAt
) {
    public static ReservationCancelResponse from(PtReservation r) {
        return new ReservationCancelResponse(r.getId(), r.getStatus().getLabel(), r.getCanceledBy().getLabel(),
                r.isDeducted(), r.isDeducted() ? 1 : 0, r.getCanceledAt());
    }
}