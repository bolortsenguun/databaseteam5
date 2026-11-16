package com.fatenexx.backend.user.dto;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.user.entity.ApprovalStatus;
import com.fatenexx.backend.user.entity.Gender;
import com.fatenexx.backend.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId, String loginId, String name, String phone,
        LocalDate birthDate, Gender gender, Role role, ApprovalStatus approvalStatus,
        String career, String introduction, LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getLoginId(), u.getName(), u.getPhone(),
                u.getBirthDate(), u.getGender(), u.getRole(), u.getApprovalStatus(),
                u.getCareer(), u.getIntroduction(), u.getCreatedAt());
    }
}