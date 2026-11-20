package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.group.entity.GroupClass;

import java.time.LocalDate;
import java.time.LocalTime;

public record GroupClassResponse(
        Long classId,
        Long trainerId,
        String name,
        String description,
        String workout,
        String imageUrl,
        LocalDate classDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Integer capacity,
        int currentParticipants,
        String status
) {
    public static GroupClassResponse of(GroupClass g, int currentParticipants) {
        return new GroupClassResponse(g.getId(), g.getTrainer().getId(), g.getName(), g.getDescription(),
                g.getWorkout(), g.getImageUrl(),                                  // [추가]
                g.getClassDate(), g.getStartTime(), g.getEndTime(), g.getCapacity(),
                currentParticipants, g.getStatus().getLabel());
    }
}