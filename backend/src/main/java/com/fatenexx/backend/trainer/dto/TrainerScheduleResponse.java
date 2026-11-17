package com.fatenexx.backend.trainer.dto;

import java.util.List;

public record TrainerScheduleResponse(
        Long trainerId,
        List<ScheduleResponse> schedules,
        List<DayOffResponse> daysOff
) {
}