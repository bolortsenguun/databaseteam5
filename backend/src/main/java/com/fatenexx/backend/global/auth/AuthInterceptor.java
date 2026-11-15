package com.fatenexx.backend.global.auth;

import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    // 승인대기 트레이너가 쓸 수 있는 주소
    private static final Set<String> PENDING_ALLOWED =
            Set.of("/api/v1/auth/me", "/api/v1/auth/logout");

    // 비번 변경 필요한 사람이 쓸 수 있는 주소
    private static final Set<String> PASSWORD_CHANGE_ALLOWED =
            Set.of("/api/v1/auth/me", "/api/v1/auth/logout", "/api/v1/auth/password");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;

        HttpSession session = request.getSession(false);
        LoginUser loginUser = (session == null) ? null : (LoginUser) session.getAttribute(SessionConst.LOGIN_USER);
        if (loginUser == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        String path = request.getRequestURI();

        if (loginUser.role() == Role.TRAINER && !loginUser.approved() && !PENDING_ALLOWED.contains(path)) {
            throw new BusinessException(ErrorCode.TRAINER_NOT_APPROVED);
        }

        if (loginUser.mustChangePassword() && !PASSWORD_CHANGE_ALLOWED.contains(path)) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        }

        Roles roles = method.getMethodAnnotation(Roles.class);
        if (roles == null) roles = method.getBeanType().getAnnotation(Roles.class);
        if (roles != null && !Arrays.asList(roles.value()).contains(loginUser.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return true;
    }
}
