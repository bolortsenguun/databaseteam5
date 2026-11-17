package com.fatenexx.backend.auth.controller;

import com.fatenexx.backend.auth.dto.ResetPasswordResponse;
import com.fatenexx.backend.auth.service.AuthService;
import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AuthService authService;

    @Roles(Role.ADMIN)
    @PostMapping("/{userId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@Login LoginUser loginUser,
                                                            @PathVariable Long userId) {
        String temporary = authService.resetPassword(loginUser.userId(), userId);
        return ApiResponse.ok(new ResetPasswordResponse(userId, temporary), "비밀번호가 초기화되었습니다.");
    }
}