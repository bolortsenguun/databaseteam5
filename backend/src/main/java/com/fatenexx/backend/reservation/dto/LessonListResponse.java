package com.fatenexx.backend.reservation.dto;

import java.util.List;

public record LessonListResponse(
        List<LessonResponse> lessons
) {
}