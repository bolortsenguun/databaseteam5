package com.fatenexx.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(
        LocalDate from,
        LocalDate to,
        long totalRevenue,
        List<MonthlyRevenueItem> monthlyRevenue,
        List<TrainerPerformanceItem> trainerPerformance,
        List<HourlyEntryItem> hourlyEntries
) {
}