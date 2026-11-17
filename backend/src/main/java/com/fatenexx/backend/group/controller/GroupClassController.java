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
import com.fatenexx.backend.group.dto.TrainerGroupClassListResponse;
import java.time.LocalDate;
import com.fatenexx.backend.group.dto.GroupClassCancelRequest;
import com.fatenexx.backend.group.dto.GroupClassCancelResponse;
import com.fatenexx.backend.group.dto.GroupClassUpdateRequest;

@RestController
@RequiredArgsConstructor
public class GroupClassController {

    private final GroupClassService groupClassService;


    // ② 트레이너 내 그룹 수업 목록
    @Roles(Role.TRAINER)
    @GetMapping("/api/v1/trainers/me/group-classes")
    public ApiResponse<TrainerGroupClassListResponse> getMyClasses(
            @Login LoginUser loginUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(groupClassService.getMyClasses(loginUser.userId(), status, startDate, endDate),
                "내 그룹 수업 목록을 조회했습니다.");
    }

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


    // ⑤ 그룹 수업 수정
    @Roles(Role.TRAINER)
    @PatchMapping("/api/v1/group-classes/{classId}")
    public ApiResponse<GroupClassResponse> update(@Login LoginUser loginUser,
                                                  @PathVariable Long classId,
                                                  @Valid @RequestBody GroupClassUpdateRequest request) {
        return ApiResponse.ok(groupClassService.update(loginUser.userId(), classId, request),
                "그룹 수업이 수정되었습니다.");
    }

    // ⑥ 그룹 수업 삭제
    @Roles(Role.TRAINER)
    @DeleteMapping("/api/v1/group-classes/{classId}")
    public ApiResponse<Void> delete(@Login LoginUser loginUser, @PathVariable Long classId) {
        groupClassService.delete(loginUser.userId(), classId);
        return ApiResponse.ok(null, "그룹 수업이 삭제되었습니다.");
    }

    // ⑦ 그룹 수업 취소
    @Roles(Role.TRAINER)
    @PostMapping("/api/v1/group-classes/{classId}/cancel")
    public ApiResponse<GroupClassCancelResponse> cancel(@Login LoginUser loginUser,
                                                        @PathVariable Long classId,
                                                        @RequestBody(required = false) GroupClassCancelRequest request) {
        return ApiResponse.ok(groupClassService.cancel(loginUser.userId(), classId, request),
                "그룹 수업이 취소되었습니다.");
    }
}