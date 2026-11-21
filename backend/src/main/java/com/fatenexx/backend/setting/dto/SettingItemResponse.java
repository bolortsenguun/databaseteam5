package com.fatenexx.backend.setting.dto;

public record SettingItemResponse(
        String settingKey,
        int settingValue,
        String description
) {
}