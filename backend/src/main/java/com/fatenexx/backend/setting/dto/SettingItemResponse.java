package com.fatenexx.backend.setting.dto;

public record SettingItemResponse(
        String key,
        int value,
        String description
) {
}