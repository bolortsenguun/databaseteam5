package com.fatenexx.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 50, message = "비밀번호는 8~50자입니다.") String newPassword
) {}