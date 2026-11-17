package com.fatenexx.backend.trainer.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.trainer.dto.TrainerListResponse;
import com.fatenexx.backend.trainer.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    // ① 회원용 트레이너 목록
    @Roles(Role.MEMBER)
    @GetMapping
    public ApiResponse<TrainerListResponse> getTrainers() {
        return ApiResponse.ok(trainerService.getTrainers(), "트레이너 목록을 조회했습니다.");
    }
}