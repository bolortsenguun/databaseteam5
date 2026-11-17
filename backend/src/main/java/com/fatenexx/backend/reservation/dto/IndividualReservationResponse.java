package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record IndividualReservationResponse(
        Long reservationId,
        Long ptPassId,
        String ptType,
        Long trainerId,
        String trainerName,
        LocalDate lessonDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String status,
        @JsonProperty("isDeducted") boolean isDeducted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime reservedAt
) {
    public static IndividualReservationResponse from(PtReservation r) {
        return new IndividualReservationResponse(r.getId(), r.getPtPass().getId(),
                r.getPtPass().getPtType().getLabel(), r.getTrainer().getId(), r.getTrainer().getName(),
                r.getLessonDate(), r.getStartTime(), r.getEndTime(), r.getStatus().getLabel(),
                r.isDeducted(), r.getReservedAt());
    }
}