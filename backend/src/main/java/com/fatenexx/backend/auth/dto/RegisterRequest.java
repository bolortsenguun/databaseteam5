package com.fatenexx.backend.auth.dto;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "아이디는 영문, 숫자, 밑줄 4~20자입니다.")
        String loginId,
        @NotBlank @Size(min = 8, max = 50, message = "비밀번호는 8~50자입니다.")
        String password,
        @NotBlank @Size(max = 50)
        String name,
        @NotBlank @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 숫자만 10~11자리입니다.")
        String phone,
        @NotNull Role role,
        LocalDate birthDate,
        Gender gender
) {}