package com.fatenexx.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    TRAINER_NOT_APPROVED(HttpStatus.FORBIDDEN, "관리자 승인 대기 중입니다."),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "비밀번호를 변경해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Auth
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INCORRECT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    SETTING_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "설정값을 찾을 수 없습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}