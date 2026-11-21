package com.fatenexx.backend.attendance.dto;

public record KioskNextLesson(
        String date,         // 11월 21일 (토)
        String time,         // 11:00
        String title,        // 그룹 수업 이름 또는 개인 PT
        String trainer
) {
}