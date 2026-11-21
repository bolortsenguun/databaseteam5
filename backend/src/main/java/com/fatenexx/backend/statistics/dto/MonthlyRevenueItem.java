package com.fatenexx.backend.statistics.dto;

public record MonthlyRevenueItem(
        String month,          // 2026-11
        long amount,           // 성공 결제 합계
        long successCount      // 성공 결제 건수
) {
}