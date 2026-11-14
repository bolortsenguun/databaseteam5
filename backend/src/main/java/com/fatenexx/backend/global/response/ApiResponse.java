package com.fatenexx.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        boolean success,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,  // 성공일 땐 code 칸을 아예 안 보여줌
        String message
) {
    // 성공 상자: ApiResponse.ok(데이터, "메시지")
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    // 실패 상자: ApiResponse.fail("에러코드", "메시지")
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }
}