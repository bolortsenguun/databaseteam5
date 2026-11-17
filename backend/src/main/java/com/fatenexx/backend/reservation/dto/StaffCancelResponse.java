package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDateTime;

public record StaffCancelResponse(
        Long reservationId,
        String status,
        String canceledBy,
        String cancelReason,
        @JsonProperty("isDeducted") boolean isDeducted,
        int deductionCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime canceledAt
) {
    public static StaffCancelResponse from(PtReservation r) {
        return new StaffCancelResponse(r.getId(), r.getStatus().getLabel(), r.getCanceledBy().getLabel(),
                r.getCancelReason(), r.isDeducted(), r.isDeducted() ? 1 : 0, r.getCanceledAt());
    }
}