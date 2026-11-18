package com.fatenexx.backend.setting.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.setting.dto.SettingListResponse;
import com.fatenexx.backend.setting.dto.UpdateSettingsRequest;
import com.fatenexx.backend.setting.service.SettingAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Roles(Role.ADMIN)
public class AdminSettingController {

    private final SettingAdminService settingAdminService;

    @GetMapping
    public ApiResponse<SettingListResponse> list() {
        return ApiResponse.ok(settingAdminService.getAll(), "조회되었습니다.");
    }

    @PatchMapping
    public ApiResponse<SettingListResponse> update(@Valid @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.ok(settingAdminService.update(request), "설정이 변경되었습니다.");
    }
}