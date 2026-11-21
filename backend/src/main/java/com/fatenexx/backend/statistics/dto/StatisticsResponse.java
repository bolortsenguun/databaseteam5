package com.fatenexx.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(
        Period period,                              // 조회 기간
        long totalRevenue,
        List<MonthlyRevenueItem> monthlyRevenue,
        List<TrainerPerformanceItem> trainerPerformance,
        List<HourlyEntryItem> hourlyCheckins        // 시간대별 입장 수 (0~23시)
) {
    public record Period(LocalDate startDate, LocalDate endDate) {
    }
}
