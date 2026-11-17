package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record GroupClassCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 200) String description,
        @NotNull LocalDate classDate,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull Integer capacity
) {
}