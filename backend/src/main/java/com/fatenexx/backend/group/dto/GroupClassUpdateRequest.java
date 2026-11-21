package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

// null = 그대로, description·workout·imageUrl "" = 삭제
public record GroupClassUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 200) String description,
        LocalDate classDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        Integer capacity,
        @Size(max = 20) String workout,     // [추가]
        String imageUrl                     // [추가]
) {
    public boolean isEmpty() {
        return name == null && description == null && classDate == null && startTime == null && capacity == null
                && workout == null && imageUrl == null;     // [추가]
    }
}