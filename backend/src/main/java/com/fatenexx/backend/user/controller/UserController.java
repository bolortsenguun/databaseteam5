package com.fatenexx.backend.user.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.user.dto.UpdateProfileRequest;
import com.fatenexx.backend.user.dto.UserResponse;
import com.fatenexx.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@Login LoginUser loginUser) {
        return ApiResponse.ok(userService.getMe(loginUser.userId()), "내 정보를 조회했습니다.");
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(@Login LoginUser loginUser,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateMe(loginUser.userId(), request), "내 정보가 수정되었습니다.");
    }
}