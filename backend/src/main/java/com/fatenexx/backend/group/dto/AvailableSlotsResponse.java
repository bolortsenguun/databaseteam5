package com.fatenexx.backend.group.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailableSlotsResponse(
        LocalDate classDate,
        List<SlotResponse> slots
) {
}