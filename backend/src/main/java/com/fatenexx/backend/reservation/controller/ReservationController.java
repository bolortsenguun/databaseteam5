package com.fatenexx.backend.reservation.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.reservation.dto.IndividualReservationRequest;
import com.fatenexx.backend.reservation.dto.IndividualReservationResponse;
import com.fatenexx.backend.reservation.dto.SlotListResponse;
import com.fatenexx.backend.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fatenexx.backend.group.service.GroupClassService;
import com.fatenexx.backend.reservation.dto.GroupReservationRequest;
import com.fatenexx.backend.reservation.dto.GroupReservationResponse;
import java.time.LocalDate;
import com.fatenexx.backend.reservation.dto.MyReservationListResponse;
import com.fatenexx.backend.reservation.dto.CancelPreviewResponse;
import com.fatenexx.backend.reservation.dto.MemberCancelRequest;
import com.fatenexx.backend.reservation.dto.ReservationCancelResponse;
import com.fatenexx.backend.reservation.dto.StaffCancelRequest;
import com.fatenexx.backend.reservation.dto.StaffCancelResponse;
import com.fatenexx.backend.reservation.dto.LessonListResponse;
import com.fatenexx.backend.reservation.dto.LessonResultRequest;
import com.fatenexx.backend.reservation.dto.LessonResultResponse;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final GroupClassService groupClassService;

    // ① 개인 PT 예약 가능 시간
    @Roles(Role.MEMBER)
    @GetMapping("/api/v1/pt-reservations/available-slots")
    public ApiResponse<SlotListResponse> getAvailableSlots(
            @Login LoginUser loginUser,
            @RequestParam Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reservationService.getAvailableSlots(loginUser.userId(), trainerId, date),
                "개인 PT 예약 가능 시간을 조회했습니다.");
    }

    // ② 개인 PT 예약 신청
    @Roles(Role.MEMBER)
    @PostMapping("/api/v1/pt-reservations/individual")
    public ResponseEntity<ApiResponse<IndividualReservationResponse>> reserveIndividual(
            @Login LoginUser loginUser, @Valid @RequestBody IndividualReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reservationService.reserveIndividual(loginUser.userId(), request),
                        "개인 PT 예약이 완료되었습니다."));
    }
    // ③ 내 PT 예약 목록
    @Roles(Role.MEMBER)
    @GetMapping("/api/v1/pt-reservations/me")
    public ApiResponse<MyReservationListResponse> getMyReservations(
            @Login LoginUser loginUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ptType) {
        return ApiResponse.ok(reservationService.getMyReservations(loginUser.userId(), status, ptType),
                "내 PT 예약 목록을 조회했습니다.");
    }

    // ④ 취소 미리보기
    @Roles(Role.MEMBER)
    @GetMapping("/api/v1/pt-reservations/{reservationId}/cancellation-preview")
    public ApiResponse<CancelPreviewResponse> previewCancel(
            @Login LoginUser loginUser, @PathVariable Long reservationId) {
        return ApiResponse.ok(reservationService.previewCancel(loginUser.userId(), reservationId),
                "예약 취소 조건을 확인했습니다.");
    }

    // ⑤ 회원 취소
    @Roles(Role.MEMBER)
    @PostMapping("/api/v1/pt-reservations/{reservationId}/cancel")
    public ApiResponse<ReservationCancelResponse> cancelByMember(
            @Login LoginUser loginUser, @PathVariable Long reservationId,
            @Valid @RequestBody MemberCancelRequest request) {
        ReservationCancelResponse res = reservationService.cancelByMember(
                loginUser.userId(), reservationId, request.acceptDeduction());
        return ApiResponse.ok(res, res.isDeducted()
                ? "PT 1회가 차감되고 예약이 취소되었습니다." : "PT 예약이 차감 없이 취소되었습니다.");
    }

    // ⑥ 그룹 PT 수업 신청
    @Roles(Role.MEMBER)
    @PostMapping("/api/v1/pt-reservations/group")
    public ResponseEntity<ApiResponse<GroupReservationResponse>> applyGroup(
            @Login LoginUser loginUser, @Valid @RequestBody GroupReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(groupClassService.apply(loginUser.userId(), request.classId()),
                        "그룹 PT 수업 신청이 완료되었습니다."));
    }

    // ⑦ 트레이너 수업 일정
    @Roles(Role.TRAINER)
    @GetMapping("/api/v1/trainers/me/lessons")
    public ApiResponse<LessonListResponse> getLessons(
            @Login LoginUser loginUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(reservationService.getLessons(loginUser.userId(), startDate, endDate, status),
                "수업 일정을 조회했습니다.");
    }

    // ⑧ 출석·결석 처리
    @Roles(Role.TRAINER)
    @PatchMapping("/api/v1/pt-reservations/{reservationId}/result")
    public ApiResponse<LessonResultResponse> recordResult(
            @Login LoginUser loginUser, @PathVariable Long reservationId,
            @RequestBody(required = false) LessonResultRequest request) {
        ReservationService.ResultOutcome out = reservationService.recordResult(
                loginUser.userId(), reservationId, request == null ? null : request.result());
        return ApiResponse.ok(out.data(), out.message());
    }

    // ⑨ 트레이너 취소
    @Roles(Role.TRAINER)
    @PostMapping("/api/v1/pt-reservations/{reservationId}/trainer-cancel")
    public ApiResponse<StaffCancelResponse> cancelByTrainer(
            @Login LoginUser loginUser, @PathVariable Long reservationId,
            @RequestBody(required = false) StaffCancelRequest request) {
        String reason = request == null ? null : request.cancelReason();
        return ApiResponse.ok(reservationService.cancelByTrainer(loginUser.userId(), reservationId, reason),
                "PT 예약이 취소되었습니다.");
    }


}