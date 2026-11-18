package com.fatenexx.backend.setting.dto;

import java.util.List;

public record SettingListResponse(
        List<SettingItemResponse> settings
) {
}