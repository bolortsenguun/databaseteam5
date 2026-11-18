package com.fatenexx.backend.setting.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SettingChangeRequest(
        @NotBlank String key,
        @NotNull @Min(0) Integer value
) {
}