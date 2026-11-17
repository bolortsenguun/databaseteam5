package com.fatenexx.backend.reservation.dto;

import com.fatenexx.backend.group.dto.SlotResponse;

import java.time.LocalDate;
import java.util.List;

public record SlotListResponse(
        Long trainerId,
        String trainerName,
        LocalDate date,
        List<SlotResponse> slots
) {
}