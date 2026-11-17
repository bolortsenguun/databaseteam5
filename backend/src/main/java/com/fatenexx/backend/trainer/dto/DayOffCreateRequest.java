package com.fatenexx.backend.trainer.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DayOffCreateRequest(
        @NotNull LocalDate offDate
) {
}