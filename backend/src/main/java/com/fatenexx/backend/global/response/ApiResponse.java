package com.fatenexx.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        boolean success,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,  // 성공일 땐 code 칸을 아예 안 보여줌
        String message
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }
}