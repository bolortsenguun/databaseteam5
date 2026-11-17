package com.fatenexx.backend.trainer.dto;

import com.fatenexx.backend.user.entity.User;

import java.util.List;

public record TrainerSummaryResponse(
        Long trainerId,
        String name,
        String career,
        String introduction,
        List<WorkTimeResponse> schedules
) {
    public static TrainerSummaryResponse of(User t, List<WorkTimeResponse> schedules) {
        return new TrainerSummaryResponse(t.getId(), t.getName(), t.getCareer(), t.getIntroduction(), schedules);
    }
}