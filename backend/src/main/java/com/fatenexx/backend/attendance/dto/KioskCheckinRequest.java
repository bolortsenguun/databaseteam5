package com.fatenexx.backend.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KioskCheckinRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
        String phone
) {
}