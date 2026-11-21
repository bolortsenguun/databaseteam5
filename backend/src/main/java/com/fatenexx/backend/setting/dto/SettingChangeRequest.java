package com.fatenexx.backend.setting.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 명세 이름(settingKey, settingValue)과 예전 이름(key, value)을 둘 다 받는다
public record SettingChangeRequest(
        @NotBlank @JsonAlias("key") String settingKey,
        @NotNull @Min(0) @JsonAlias("value") Integer settingValue
) {
}
