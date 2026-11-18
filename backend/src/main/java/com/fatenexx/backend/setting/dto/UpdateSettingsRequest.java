package com.fatenexx.backend.setting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateSettingsRequest(
        @NotEmpty @Valid List<SettingChangeRequest> settings
) {
}