package com.fatenexx.backend.attendance.controller;

import com.fatenexx.backend.attendance.dto.AdminAttendanceListResponse;
import com.fatenexx.backend.attendance.service.AttendanceService;
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
@RequestMapping("/api/v1/admin/attendances")
@RequiredArgsConstructor
@Roles(Role.ADMIN)
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ApiResponse<AdminAttendanceListResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.getAttendancesByDate(date), "조회되었습니다.");
    }
}