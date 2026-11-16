package com.fatenexx.backend.user.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.user.dto.MemberListResponse;
import com.fatenexx.backend.user.dto.MemberSummaryResponse;
import com.fatenexx.backend.user.dto.UpdateProfileRequest;
import com.fatenexx.backend.user.dto.UserResponse;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    // 내 정보 수정 (회원, 트레이너, 관리자 모두)
    @Transactional
    public UserResponse updateMe(Long userId, UpdateProfileRequest req) {
        User user = findUser(userId);
        boolean wantsTrainerFields = req.career() != null || req.introduction() != null;
        if (wantsTrainerFields && user.getRole() != Role.TRAINER) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경력과 소개글은 트레이너만 입력할 수 있습니다.");
        }
        applyBasicInfo(user, req);
        user.updateTrainerProfile(req.career(), req.introduction());
        return UserResponse.from(user);
    }

    // 관리자: 회원 목록 (이름·전화번호·아이디 검색)
    public MemberListResponse getMembers(String keyword, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100이어야 합니다.");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        var result = userRepository.searchByRole(Role.MEMBER, kw,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return MemberListResponse.from(result.map(MemberSummaryResponse::from));
    }

    // 관리자: 회원 상세 (현재는 기본 정보만, 이용권·예약·출석은 해당 기능 완성 후 추가)
    public UserResponse getMember(Long memberId) {
        return UserResponse.from(findMember(memberId));
    }

    // 관리자: 회원 정보 수정 (아이디는 수정 불가, BR-ACC-08)
    @Transactional
    public UserResponse updateMember(Long memberId, UpdateProfileRequest req) {
        if (req.career() != null || req.introduction() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "회원에게는 경력과 소개글이 없습니다.");
        }
        User member = findMember(memberId);
        applyBasicInfo(member, req);
        return UserResponse.from(member);
    }

    private void applyBasicInfo(User user, UpdateProfileRequest req) {
        if (req.phone() != null && !req.phone().equals(user.getPhone())
                && userRepository.existsByPhoneAndIdNot(req.phone(), user.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }
        user.updateProfile(req.name(), req.phone(), req.birthDate(), req.gender());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 회원(MEMBER)만 조회 대상. 트레이너·관리자 ID는 "없음"으로 처리
    private User findMember(Long memberId) {
        return userRepository.findByIdAndRole(memberId, Role.MEMBER)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}