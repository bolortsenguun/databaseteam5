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
import com.fatenexx.backend.trainer.dto.ScheduleCreateRequest;
import com.fatenexx.backend.trainer.dto.ScheduleCreateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    // ⑦ 근무 일정 추가
    @Roles({Role.TRAINER, Role.ADMIN})
    @PostMapping("/schedules")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> addSchedule(@Login LoginUser loginUser,
                                                                           @PathVariable Long trainerId,
                                                                           @Valid @RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(trainerScheduleService.addSchedule(loginUser, trainerId, request),
                        "근무 일정이 등록되었습니다."));
    }
}