package com.fatenexx.backend.pass.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.pass.dto.MyPassesResponse;
import com.fatenexx.backend.pass.service.PassService;
// ↓ ApiResponse, Login, LoginUser, Roles, Role 은 PaymentController 의 import 와 똑같이 맞추세요
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passes")
@RequiredArgsConstructor
@Roles(Role.MEMBER)
public class PassController {

    private final PassService passService;

    @GetMapping("/me")
    public ApiResponse<MyPassesResponse> myPasses(@Login LoginUser loginUser) {
        return ApiResponse.ok(passService.getMyPasses(loginUser.userId()), "조회되었습니다.");
    }
}