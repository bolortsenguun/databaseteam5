package com.fatenexx.backend.trainer.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.trainer.dto.AdminTrainerListResponse;
import com.fatenexx.backend.trainer.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.fatenexx.backend.trainer.dto.TrainerApproveResponse;
import com.fatenexx.backend.trainer.dto.TrainerRejectResponse;

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


    // ③ 트레이너 가입 승인
    @PostMapping("/{trainerId}/approve")
    public ApiResponse<TrainerApproveResponse> approve(@PathVariable Long trainerId) {
        return ApiResponse.ok(trainerService.approve(trainerId), "트레이너 가입이 승인되었습니다.");
    }

    // ④ 트레이너 가입 거절
    @PostMapping("/{trainerId}/reject")
    public ApiResponse<TrainerRejectResponse> reject(@PathVariable Long trainerId) {
        return ApiResponse.ok(trainerService.reject(trainerId), "트레이너 가입이 거절되었습니다.");
    }
}