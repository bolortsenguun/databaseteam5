package com.fatenexx.backend.trainer.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.trainer.dto.AdminTrainerListResponse;
import com.fatenexx.backend.trainer.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/trainers")
@Roles(Role.ADMIN)
public class AdminTrainerController {

    private final TrainerService trainerService;

    // ② 관리자 트레이너 목록
    @GetMapping
    public ApiResponse<AdminTrainerListResponse> getTrainers(
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(trainerService.getTrainersForAdmin(approvalStatus, page, size),
                "트레이너 목록을 조회했습니다.");
    }
}