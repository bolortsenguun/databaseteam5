package com.fatenexx.backend.statistics.dto;

public record HourlyEntryItem(
        int hour,              // 0 ~ 23
        long count
) {
}