package com.fatenexx.backend.trainer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.trainer.entity.TrainerSchedule;

import java.time.LocalTime;

public record WorkTimeResponse(
        Integer dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
    public static WorkTimeResponse from(TrainerSchedule s) {
        return new WorkTimeResponse(s.getDayOfWeek(), s.getStartTime(), s.getEndTime());
    }
}