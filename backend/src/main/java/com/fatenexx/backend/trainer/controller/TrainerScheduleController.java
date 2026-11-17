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
import com.fatenexx.backend.trainer.dto.DayOffCreateRequest;
import com.fatenexx.backend.trainer.dto.DayOffCreateResponse;


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

    // ⑧ 근무 일정 삭제
    @Roles({Role.TRAINER, Role.ADMIN})
    @DeleteMapping("/schedules/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(@Login LoginUser loginUser,
                                            @PathVariable Long trainerId,
                                            @PathVariable Long scheduleId) {
        trainerScheduleService.deleteSchedule(loginUser, trainerId, scheduleId);
        return ApiResponse.ok(null, "근무 일정이 삭제되었습니다.");
    }

    // ⑨ 휴무 추가
    @Roles({Role.TRAINER, Role.ADMIN})
    @PostMapping("/days-off")
    public ResponseEntity<ApiResponse<DayOffCreateResponse>> addDayOff(@Login LoginUser loginUser,
                                                                       @PathVariable Long trainerId,
                                                                       @Valid @RequestBody DayOffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(trainerScheduleService.addDayOff(loginUser, trainerId, request),
                        "휴무일이 등록되었습니다."));
    }
}