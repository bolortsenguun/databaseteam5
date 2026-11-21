package com.fatenexx.backend.setting.dto;

import java.util.Map;

// 회원·트레이너·관리자 공용 설정 조회 응답 (키 → 값)
public record PublicSettingsResponse(Map<String, Integer> settings) {
}