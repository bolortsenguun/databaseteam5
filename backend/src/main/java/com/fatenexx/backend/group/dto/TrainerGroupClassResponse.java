package com.fatenexx.backend.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.group.entity.GroupClass;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TrainerGroupClassResponse(
        Long classId,
        String name,
        String description,
        LocalDate classDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Integer capacity,
        int currentParticipants,
        String status,
        List<ParticipantResponse> participants
) {
    public static TrainerGroupClassResponse of(GroupClass g, List<ParticipantResponse> participants) {
        return new TrainerGroupClassResponse(g.getId(), g.getName(), g.getDescription(), g.getClassDate(),
                g.getStartTime(), g.getEndTime(), g.getCapacity(), participants.size(),
                g.getStatus().getLabel(), participants);
    }
}