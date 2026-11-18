package com.fatenexx.backend.attendance.controller;

import com.fatenexx.backend.attendance.dto.AttendancePageResponse;
import com.fatenexx.backend.attendance.service.AttendanceService;
import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
@Roles(Role.MEMBER)
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/me")
    public ApiResponse<AttendancePageResponse> myAttendances(
            @Login LoginUser loginUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                attendanceService.getMyAttendances(loginUser.userId(), from, to, page, size),
                "조회되었습니다.");
    }
}