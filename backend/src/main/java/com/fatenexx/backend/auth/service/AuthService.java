package com.fatenexx.backend.auth.service;

import com.fatenexx.backend.auth.dto.RegisterRequest;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 계정은 가입할 수 없습니다.");
        }
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }
        User user = User.register(req.loginId(), passwordEncoder.encode(req.password()),
                req.role(), req.name(), req.phone(), req.birthDate(), req.gender());
        return userRepository.save(user);
    }

    public User authenticate(String loginId, String password) {
        // 아이디가 없든 비밀번호가 틀리든 같은 에러 (계정 존재 여부를 숨김)
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return user;
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    // 없으면 예외
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 없을 수 있는 경우용 (예: 가입이 거절되어 계정이 삭제된 트레이너의 세션)
    public Optional<User> findUser(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INCORRECT_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        return user;
    }

    @Transactional
    public String resetPassword(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "본인 계정은 초기화할 수 없습니다.");
        }
        User target = getUser(targetUserId);
        String temporary = generateTemporaryPassword();
        target.resetPassword(passwordEncoder.encode(temporary));
        return temporary;
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(TEMP_CHARS.charAt(random.nextInt(TEMP_CHARS.length())));
        }
        return sb.toString();
    }
}