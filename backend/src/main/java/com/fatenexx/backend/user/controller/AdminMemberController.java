package com.fatenexx.backend.user.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.user.dto.AdminMemberDetailResponse;
import com.fatenexx.backend.user.dto.MemberListResponse;
import com.fatenexx.backend.user.dto.UpdateProfileRequest;
import com.fatenexx.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Roles(Role.ADMIN)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<MemberListResponse> getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String membershipStatus,   // 이용중 / 미이용 (없으면 전체)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.getMembers(keyword, membershipStatus, page, size),
                "회원 목록을 조회했습니다.");
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminMemberDetailResponse> getMember(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getMember(userId), "회원 정보를 조회했습니다.");
    }

    @PatchMapping("/{userId}")
    public ApiResponse<AdminMemberDetailResponse> updateMember(@PathVariable Long userId,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateMember(userId, request), "회원 정보가 수정되었습니다.");
    }
}