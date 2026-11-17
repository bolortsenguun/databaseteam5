package com.fatenexx.backend.trainer.dto;

import com.fatenexx.backend.user.entity.User;

import java.time.LocalDate;

public record AdminTrainerDetailResponse(
        Long trainerId,
        String loginId,
        String name,
        String phone,
        LocalDate birthDate,
        String gender,
        String career,
        String introduction,
        String approvalStatus
) {
    public static AdminTrainerDetailResponse from(User t) {
        return new AdminTrainerDetailResponse(t.getId(), t.getLoginId(), t.getName(), t.getPhone(),
                t.getBirthDate(),
                t.getGender() == null ? null : t.getGender().getLabel(),
                t.getCareer(), t.getIntroduction(),
                t.getApprovalStatus().getLabel());
    }
}