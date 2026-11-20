package com.fatenexx.backend.attendance.dto;

import java.time.LocalDate;

public record KioskMembershipInfo(
        String name,
        int daysLeft,        // 종료일까지 남은 날 (종료 당일은 0)
        int totalDays,       // 시작일 ~ 종료일 (양 끝 포함)
        LocalDate endDate
) {
}