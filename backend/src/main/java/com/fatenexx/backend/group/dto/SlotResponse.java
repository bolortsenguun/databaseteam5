package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record SlotResponse(
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        boolean available,
        String unavailableReason
) {
}