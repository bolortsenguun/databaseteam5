package com.fatenexx.backend.group.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.group.dto.AvailableSlotsResponse;
import com.fatenexx.backend.group.dto.GroupClassCreateRequest;
import com.fatenexx.backend.group.dto.GroupClassResponse;
import com.fatenexx.backend.group.service.GroupClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class GroupClassController {

    private final GroupClassService groupClassService;

    // ③ 그룹 수업 개설 가능 시간
    @Roles(Role.TRAINER)
    @GetMapping("/api/v1/trainers/me/group-class-available-slots")
    public ApiResponse<AvailableSlotsResponse> getAvailableSlots(
            @Login LoginUser loginUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate classDate) {
        return ApiResponse.ok(groupClassService.getAvailableSlots(loginUser.userId(), classDate),
                "그룹 수업 개설 가능 시간을 조회했습니다.");
    }

    // ④ 그룹 수업 개설
    @Roles(Role.TRAINER)
    @PostMapping("/api/v1/group-classes")
    public ResponseEntity<ApiResponse<GroupClassResponse>> create(@Login LoginUser loginUser,
                                                                  @Valid @RequestBody GroupClassCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(groupClassService.create(loginUser.userId(), request),
                        "그룹 수업이 개설되었습니다."));
    }
}