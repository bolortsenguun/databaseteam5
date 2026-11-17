package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MyReservationResponse(
        Long reservationId,
        Long ptPassId,
        String ptType,
        Long groupClassId,
        String className,
        Long trainerId,
        String trainerName,
        LocalDate lessonDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String status,
        @JsonProperty("isDeducted") boolean isDeducted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime reservedAt,
        String canceledBy,
        String cancelReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime canceledAt
) {
    public static MyReservationResponse from(PtReservation r) {
        boolean group = r.getGroupClass() != null;
        return new MyReservationResponse(r.getId(), r.getPtPass().getId(), r.getPtPass().getPtType().getLabel(),
                group ? r.getGroupClass().getId() : null,
                group ? r.getGroupClass().getName() : null,
                r.getTrainer().getId(), r.getTrainer().getName(), r.getLessonDate(),
                r.getStartTime(), r.getEndTime(), r.getStatus().getLabel(), r.isDeducted(), r.getReservedAt(),
                r.getCanceledBy() != null ? r.getCanceledBy().getLabel() : null,
                r.getCancelReason(), r.getCanceledAt());
    }
}