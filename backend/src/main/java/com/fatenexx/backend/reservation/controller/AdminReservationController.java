package com.fatenexx.backend.reservation.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.reservation.dto.StaffCancelRequest;
import com.fatenexx.backend.reservation.dto.StaffCancelResponse;
import com.fatenexx.backend.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.fatenexx.backend.reservation.dto.AdminReservationListResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;


@RestController
@RequiredArgsConstructor
@Roles(Role.ADMIN)
public class AdminReservationController {

    private final ReservationService reservationService;

    // ⑩ 관리자 예약 목록
    @GetMapping("/api/v1/admin/pt-reservations")
    public ApiResponse<AdminReservationListResponse> getReservations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) String ptType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservationService.getReservationsForAdmin(
                        userId, trainerId, ptType, status, startDate, endDate, page, size),
                "전체 PT 예약 목록을 조회했습니다.");
    }

    // ⑪ 관리자 취소
    @PostMapping("/api/v1/admin/pt-reservations/{reservationId}/cancel")
    public ApiResponse<StaffCancelResponse> cancelByAdmin(
            @PathVariable Long reservationId,
            @RequestBody(required = false) StaffCancelRequest request) {
        String reason = request == null ? null : request.cancelReason();
        return ApiResponse.ok(reservationService.cancelByAdmin(reservationId, reason),
                "PT 예약이 관리자에 의해 취소되었습니다.");
    }
}