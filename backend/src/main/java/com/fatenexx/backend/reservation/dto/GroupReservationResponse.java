package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record GroupReservationResponse(
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
        int currentParticipants,
        int capacity
) {
    public static GroupReservationResponse of(PtReservation r, int currentParticipants) {
        return new GroupReservationResponse(r.getId(), r.getPtPass().getId(),
                r.getPtPass().getPtType().getLabel(), r.getGroupClass().getId(), r.getGroupClass().getName(),
                r.getTrainer().getId(), r.getTrainer().getName(), r.getLessonDate(),
                r.getStartTime(), r.getEndTime(), r.getStatus().getLabel(), r.isDeducted(),
                r.getReservedAt(), currentParticipants, r.getGroupClass().getCapacity());
    }
}