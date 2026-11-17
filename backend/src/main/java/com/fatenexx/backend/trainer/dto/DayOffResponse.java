package com.fatenexx.backend.trainer.dto;

import com.fatenexx.backend.trainer.entity.TrainerDayOff;

import java.time.LocalDate;

public record DayOffResponse(
        Long dayOffId,
        LocalDate offDate
) {
    public static DayOffResponse from(TrainerDayOff d) {
        return new DayOffResponse(d.getId(), d.getOffDate());
    }
}