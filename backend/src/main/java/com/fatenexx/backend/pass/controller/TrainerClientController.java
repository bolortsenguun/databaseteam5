package com.fatenexx.backend.pass.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.pass.dto.ClientListResponse;
import com.fatenexx.backend.pass.service.PassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainers/me")
@RequiredArgsConstructor
@Roles(Role.TRAINER)
public class TrainerClientController {

    private final PassService passService;

    @GetMapping("/clients")
    public ApiResponse<ClientListResponse> myClients(@Login LoginUser loginUser) {
        return ApiResponse.ok(passService.getMyClients(loginUser.userId()), "조회되었습니다.");
    }
}