package com.fatenexx.backend.auth.controller;

import com.fatenexx.backend.auth.dto.AuthUserResponse;
import com.fatenexx.backend.auth.dto.LoginRequest;
import com.fatenexx.backend.auth.dto.PasswordChangeRequest;
import com.fatenexx.backend.auth.dto.RegisterRequest;
import com.fatenexx.backend.auth.service.AuthService;
import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.SessionConst;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ApiResponse.ok(AuthUserResponse.from(user), "가입이 완료되었습니다.");
    }

    @GetMapping("/check-login-id")
    public ApiResponse<Map<String, Object>> checkLoginId(@RequestParam String loginId) {
        if (!loginId.matches("^[A-Za-z0-9_]{4,20}$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아이디는 영문, 숫자, 밑줄 4~20자로 입력해 주세요.");
        }
        return ApiResponse.ok(
                Map.of("loginId", loginId, "available", authService.isLoginIdAvailable(loginId)),
                "아이디 확인이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthUserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        User user = authService.authenticate(request.loginId(), request.password());
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();   // 세션 고정 공격 방지
        session.setAttribute(SessionConst.LOGIN_USER, toLoginUser(user));
        return ApiResponse.ok(AuthUserResponse.from(user), "로그인되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ApiResponse.ok(null, "로그아웃되었습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@Login LoginUser loginUser) {
        User user = authService.getUser(loginUser.userId());
        return ApiResponse.ok(AuthUserResponse.from(user), "로그인 상태입니다.");
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@Login LoginUser loginUser,
                                            @Valid @RequestBody PasswordChangeRequest request,
                                            HttpServletRequest httpRequest) {
        User user = authService.changePassword(loginUser.userId(),
                request.currentPassword(), request.newPassword());
        // 세션 정보 갱신 (비밀번호 변경 필요 상태 해제)
        httpRequest.getSession().setAttribute(SessionConst.LOGIN_USER, toLoginUser(user));
        return ApiResponse.ok(null, "비밀번호가 변경되었습니다.");
    }

    private LoginUser toLoginUser(User user) {
        return new LoginUser(user.getId(), user.getRole(), user.isApproved(), user.isMustChangePassword());
    }
}