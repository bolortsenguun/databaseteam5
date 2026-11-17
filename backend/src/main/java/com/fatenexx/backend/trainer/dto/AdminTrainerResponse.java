package com.fatenexx.backend.trainer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTrainerResponse(
        Long trainerId,
        String loginId,
        String name,
        String phone,
        String approvalStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        long clientCount,
        List<WorkTimeResponse> schedules
) {
    public static AdminTrainerResponse of(User t, long clientCount, List<WorkTimeResponse> schedules) {
        return new AdminTrainerResponse(t.getId(), t.getLoginId(), t.getName(), t.getPhone(),
                t.getApprovalStatus().getLabel(), t.getCreatedAt(), clientCount, schedules);
    }
}