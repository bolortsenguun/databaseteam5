package com.fatenexx.backend.user.dto;

import com.fatenexx.backend.user.entity.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 보낸 값만 수정한다. 보내지 않은(null) 값은 그대로 둔다.
public record UpdateProfileRequest(
        @Size(min = 1, max = 50) String name,
        @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 숫자만 10~11자리입니다.") String phone,
        LocalDate birthDate,
        Gender gender,
        @Size(max = 200) String career,
        @Size(max = 1000) String introduction
) {}