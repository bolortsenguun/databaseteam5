package com.fatenexx.backend.user.service;

import com.fatenexx.backend.attendance.dto.AttendanceItemResponse;
import com.fatenexx.backend.attendance.repository.AttendanceRepository;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.user.dto.MemberListResponse;
import com.fatenexx.backend.user.dto.MemberSummaryResponse;
import com.fatenexx.backend.user.dto.UpdateProfileRequest;
import com.fatenexx.backend.user.dto.UserResponse;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import com.fatenexx.backend.pass.repository.MembershipRepository;
import com.fatenexx.backend.pass.service.PassService;
import com.fatenexx.backend.user.dto.AdminMemberDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final AttendanceRepository attendanceRepository;
    private final PassService passService;

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
    public MemberListResponse getMembers(String keyword, String membershipStatus, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 1 이상, size는 1~100이어야 합니다.");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Boolean active = parseMembershipStatus(membershipStatus);
        LocalDate today = LocalDate.now(KST);

        Page<User> result = userRepository.searchMembers(Role.MEMBER, kw, active, today,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));

        // 이번 페이지 회원들의 이용 상태를 쿼리 한 번으로 조회
        List<Long> ids = result.getContent().stream().map(User::getId).toList();
        Set<Long> activeIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(membershipRepository.findActiveUserIds(ids, today));

        return MemberListResponse.from(
                result.map(u -> MemberSummaryResponse.from(u, activeIds.contains(u.getId()))));
    }

    // 이용중 → true, 미이용 → false, 없음·전체 → null(조건 없음)
    private Boolean parseMembershipStatus(String status) {
        if (status == null || status.isBlank() || status.equals("전체")) return null;
        if (status.equals(MemberSummaryResponse.ACTIVE)) return true;
        if (status.equals(MemberSummaryResponse.INACTIVE)) return false;
        throw new BusinessException(ErrorCode.INVALID_INPUT, "membershipStatus는 이용중 또는 미이용이어야 합니다.");
    }

    // 관리자: 회원 상세 (기본 정보 + 회원권 + PT 이용권 + 최근 출석 10건)
    public AdminMemberDetailResponse getMember(Long memberId) {
        return toDetail(findMember(memberId));
    }

    // 관리자: 회원 정보 수정 (아이디는 수정 불가, BR-ACC-08)
    // 수정 후에도 화면의 이용권·출석 표가 유지되도록 상세와 같은 모양으로 돌려준다
    @Transactional
    public AdminMemberDetailResponse updateMember(Long memberId, UpdateProfileRequest req) {
        if (req.career() != null || req.introduction() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "회원에게는 경력과 소개글이 없습니다.");
        }
        User member = findMember(memberId);
        applyBasicInfo(member, req);
        return toDetail(member);
    }

    // 회원권·PT 이용권은 회원 본인 화면(/passes/me)과 같은 로직을 그대로 쓴다
    private AdminMemberDetailResponse toDetail(User member) {
        List<AttendanceItemResponse> recentAttendances = attendanceRepository
                .findTop10ByUserIdOrderByCheckedInAtDesc(member.getId()).stream()
                .map(a -> new AttendanceItemResponse(a.getId(), a.getCheckedInAt(),
                        a.getEntryType().getLabel(),
                        a.getReservation() == null ? null : a.getReservation().getId()))
                .toList();
        return AdminMemberDetailResponse.of(member, passService.getMyPasses(member.getId()), recentAttendances);
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