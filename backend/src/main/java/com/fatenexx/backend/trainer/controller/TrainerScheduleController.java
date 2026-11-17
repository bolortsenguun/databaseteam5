package com.fatenexx.backend.trainer.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.trainer.dto.TrainerScheduleResponse;
import com.fatenexx.backend.trainer.service.TrainerScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trainers/{trainerId}")
public class TrainerScheduleController {

    private final TrainerScheduleService trainerScheduleService;

    // ⑥ 근무 일정 조회
    @Roles({Role.TRAINER, Role.ADMIN})
    @GetMapping("/schedules")
    public ApiResponse<TrainerScheduleResponse> getSchedules(@Login LoginUser loginUser,
                                                             @PathVariable Long trainerId) {
        return ApiResponse.ok(trainerScheduleService.getSchedules(loginUser, trainerId),
                "근무 일정을 조회했습니다.");
    }
}