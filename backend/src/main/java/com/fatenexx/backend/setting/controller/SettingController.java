package com.fatenexx.backend.setting.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.setting.dto.PublicSettingsResponse;
import com.fatenexx.backend.setting.service.SettingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 운영 설정 읽기 전용 (로그인한 모든 역할). 변경은 관리자 API(/admin/settings)에서만
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Roles({Role.MEMBER, Role.TRAINER, Role.ADMIN})
public class SettingController {

    private final SettingAdminService settingAdminService;

    @GetMapping
    public ApiResponse<PublicSettingsResponse> list() {
        return ApiResponse.ok(settingAdminService.getPublicSettings(), "조회되었습니다.");
    }
}