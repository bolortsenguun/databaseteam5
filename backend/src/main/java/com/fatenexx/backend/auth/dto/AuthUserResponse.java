package com.fatenexx.backend.auth.dto;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.user.entity.ApprovalStatus;
import com.fatenexx.backend.user.entity.User;

public record AuthUserResponse(
        Long userId,
        String loginId,
        String name,
        Role role,
        ApprovalStatus approvalStatus,
        boolean mustChangePassword
) {
    public static AuthUserResponse from(User u) {
        return new AuthUserResponse(u.getId(), u.getLoginId(), u.getName(),
                u.getRole(), u.getApprovalStatus(), u.isMustChangePassword());
    }
}