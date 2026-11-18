package com.fatenexx.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.attendance.dto.AttendanceItemResponse;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.pass.dto.MembershipItemResponse;
import com.fatenexx.backend.pass.dto.MyPassesResponse;
import com.fatenexx.backend.pass.dto.PtPassItemResponse;
import com.fatenexx.backend.user.entity.ApprovalStatus;
import com.fatenexx.backend.user.entity.Gender;
import com.fatenexx.backend.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 관리자: 회원 상세 (기본 정보 + 회원권 + PT 이용권 + 최근 출석)
public record AdminMemberDetailResponse(
        Long userId, String loginId, String name, String phone,
        LocalDate birthDate, Gender gender, Role role, ApprovalStatus approvalStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        List<MembershipItemResponse> memberships,
        List<PtPassItemResponse> ptPasses,
        List<AttendanceItemResponse> recentAttendances     // 최근 10건, 최신순
) {
    public static AdminMemberDetailResponse of(User u, MyPassesResponse passes,
                                               List<AttendanceItemResponse> recentAttendances) {
        return new AdminMemberDetailResponse(u.getId(), u.getLoginId(), u.getName(), u.getPhone(),
                u.getBirthDate(), u.getGender(), u.getRole(), u.getApprovalStatus(),
                u.getCreatedAt(),
                passes.memberships(), passes.ptPasses(), recentAttendances);
    }
}
