package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record IndividualReservationRequest(
        @NotNull Long trainerId,
        @NotNull LocalDate lessonDate,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime
) {
}