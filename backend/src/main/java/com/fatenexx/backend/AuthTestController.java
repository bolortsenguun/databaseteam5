package com.fatenexx.backend;

import com.fatenexx.backend.global.auth.*;
import com.fatenexx.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
// TODO: 로그인 API 완성 후 삭제 (아무나 관리자로 로그인할 수 있음)
@RestController
public class AuthTestController {

    // 가짜 로그인: 진짜 로그인 API 만들기 전까지 시험용
    @GetMapping("/api/v1/health/fake-login")
    public ApiResponse<LoginUser> fakeLogin(@RequestParam Role role,
                                            @RequestParam(defaultValue = "true") boolean approved,
                                            @RequestParam(defaultValue = "false") boolean mustChange,
                                            HttpSession session) {
        LoginUser user = new LoginUser(1L, role, approved, mustChange);
        session.setAttribute(SessionConst.LOGIN_USER, user);
        return ApiResponse.ok(user, "가짜 로그인 완료");
    }

    // 로그인해야 들어올 수 있음
    @GetMapping("/api/v1/test/me")
    public ApiResponse<LoginUser> me(@Login LoginUser loginUser) {
        return ApiResponse.ok(loginUser, "로그인 상태");
    }

    // 관리자만 들어올 수 있음
    @Roles(Role.ADMIN)
    @GetMapping("/api/v1/test/admin")
    public ApiResponse<String> adminOnly() {
        return ApiResponse.ok("관리자 전용", "통과");
    }
}