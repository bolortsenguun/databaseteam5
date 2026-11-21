package com.fatenexx.backend.attendance.controller;

import com.fatenexx.backend.attendance.dto.KioskCheckinRequest;
import com.fatenexx.backend.attendance.dto.KioskCheckinResponse;
import com.fatenexx.backend.attendance.service.AttendanceService;
import com.fatenexx.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkins")
    public ResponseEntity<ApiResponse<KioskCheckinResponse>> checkin(
            @Valid @RequestBody KioskCheckinRequest request) {
        KioskCheckinResponse result = attendanceService.checkin(request.phone());
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.ok(result, result.message()));
    }
}