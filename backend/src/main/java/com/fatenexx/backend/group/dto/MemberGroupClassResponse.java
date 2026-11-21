package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.group.entity.GroupClass;

import java.time.LocalDate;
import java.time.LocalTime;

public record MemberGroupClassResponse(
        Long classId,
        String name,
        String description,
        String workout,
        String imageUrl,
        Long trainerId,
        String trainerName,
        LocalDate classDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Integer capacity,
        int currentParticipants,
        int remainingSeats,
        String status,
        boolean canReserve,
        String unavailableReason
) {
    public static MemberGroupClassResponse of(GroupClass g, int current, String reason) {
        return new MemberGroupClassResponse(g.getId(), g.getName(), g.getDescription(),
                g.getWorkout(), g.getImageUrl(),                                  // [추가]
                g.getTrainer().getId(), g.getTrainer().getName(),
                g.getClassDate(), g.getStartTime(), g.getEndTime(), g.getCapacity(),
                current, g.getCapacity() - current, g.getStatus().getLabel(),
                reason == null, reason);
    }
}