package com.fatenexx.backend.statistics.dto;

public record MonthlyRevenueItem(
        String month,          // 2026-11
        long revenue,
        long paymentCount
) {
}