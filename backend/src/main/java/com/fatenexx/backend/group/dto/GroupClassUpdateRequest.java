package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

// null = 그대로, description "" = 삭제
public record GroupClassUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 200) String description,
        LocalDate classDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        Integer capacity
) {
    public boolean isEmpty() {
        return name == null && description == null && classDate == null && startTime == null && capacity == null;
    }
}