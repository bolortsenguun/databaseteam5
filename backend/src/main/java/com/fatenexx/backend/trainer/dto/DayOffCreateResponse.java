package com.fatenexx.backend.trainer.dto;

import com.fatenexx.backend.trainer.entity.TrainerDayOff;

import java.time.LocalDate;

public record DayOffCreateResponse(
        Long dayOffId,
        Long trainerId,
        LocalDate offDate
) {
    public static DayOffCreateResponse from(TrainerDayOff d, Long trainerId) {
        return new DayOffCreateResponse(d.getId(), trainerId, d.getOffDate());
    }
}