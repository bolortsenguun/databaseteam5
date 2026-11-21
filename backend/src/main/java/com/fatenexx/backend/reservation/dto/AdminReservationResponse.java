package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminReservationResponse(
        Long reservationId,
        Long userId,
        String userName,
        String userPhone,
        Long trainerId,
        String trainerName,
        String ptType,
        Long ptPassId,
        Long groupClassId,
        String className,
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
    public static AdminReservationResponse from(PtReservation r) {
        boolean group = r.getGroupClass() != null;
        return new AdminReservationResponse(r.getId(), r.getUser().getId(), r.getUser().getName(),
                r.getUser().getPhone(),                                          // [추가]
                r.getTrainer().getId(), r.getTrainer().getName(), r.getPtPass().getPtType().getLabel(),
                r.getPtPass().getId(),
                group ? r.getGroupClass().getId() : null,
                group ? r.getGroupClass().getName() : null,
                r.getLessonDate(), r.getStartTime(), r.getEndTime(), r.getStatus().getLabel(), r.isDeducted(),
                r.getReservedAt(),
                r.getCanceledBy() != null ? r.getCanceledBy().getLabel() : null,
                r.getCancelReason(), r.getCanceledAt());
    }
}