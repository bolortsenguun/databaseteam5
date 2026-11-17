package com.fatenexx.backend.trainer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.trainer.entity.TrainerSchedule;

import java.time.LocalTime;

public record ScheduleCreateResponse(
        Long scheduleId,
        Long trainerId,
        Integer dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
    public static ScheduleCreateResponse from(TrainerSchedule s, Long trainerId) {
        return new ScheduleCreateResponse(s.getId(), trainerId, s.getDayOfWeek(),
                s.getStartTime(), s.getEndTime());
    }
}