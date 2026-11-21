package com.fatenexx.backend.attendance.service;

import com.fatenexx.backend.attendance.dto.*;
import com.fatenexx.backend.attendance.entity.Attendance;
import com.fatenexx.backend.attendance.entity.EntryType;
import com.fatenexx.backend.attendance.repository.AttendanceRepository;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.pass.entity.Membership;
import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.pass.repository.MembershipRepository;
import com.fatenexx.backend.pass.repository.PtPassRepository;
import com.fatenexx.backend.reservation.entity.PtReservation;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.reservation.repository.PtReservationRepository;
import com.fatenexx.backend.setting.SettingKey;
import com.fatenexx.backend.setting.service.SettingService;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LESSON_DATE =                       // [추가]
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);          // [추가]
    private static final String NO_PASS_MESSAGE = "입장 가능한 이용권이 없습니다. 데스크에 문의해 주세요.";
    // [추가] 횟수를 미리 확보하고 있는 예약 상태 (아직 차감은 안 됨)
    private static final List<ReservationStatus> HOLDING =
            List.of(ReservationStatus.RESERVED, ReservationStatus.PENDING_RESULT);

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PtPassRepository ptPassRepository;                           // [추가]
    private final PtReservationRepository ptReservationRepository;
    private final SettingService settingService;

    /** 키오스크 체크인 (로그인 없이 전화번호로) */
    @Transactional
    public KioskCheckinResponse checkin(String phone) {
        // 없는 번호·회원이 아닌 번호도 '이용권 없음'과 같은 403으로 답한다
        // → 번호만 넣어 보고 회원인지 알아낼 수 없게 (처리 규칙 19번)
        User found = userRepository.findByPhone(phone)
                .filter(u -> u.getRole() == Role.MEMBER)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_ALLOWED, NO_PASS_MESSAGE));

        // 같은 회원이 동시에 두 번 눌러도 중복 기록되지 않도록 회원 행을 잠근다
        User user = userRepository.findByIdForUpdate(found.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        String maskedName = maskName(user.getName());

        // 1) 10분 안 재입장은 기록하지 않는다
        int recheckMinutes = settingService.getInt(SettingKey.RECHECKIN_MINUTES);
        Optional<Attendance> last = attendanceRepository.findFirstByUserIdOrderByCheckedInAtDesc(user.getId());
        if (last.isPresent() && last.get().getCheckedInAt().isAfter(now.minusMinutes(recheckMinutes))) {
            Attendance a = last.get();
            PtReservation lastReservation = a.getReservation();                // [추가]
            return buildResponse(user, maskedName, a.getEntryType().getLabel(),
                    a.getReservation() == null ? null : a.getReservation().getId(),
                    a.getCheckedInAt(), false, "이미 입장 처리되었습니다.",
                    lastReservation == null ? null : lastReservation.getPtPass());
        }

        // 2) 오늘 유효한 회원권이 있으면 회원권 입장 (회원권과 PT 예약이 둘 다 있어도 회원권 우선, 처리 규칙 9번)
        if (membershipRepository.existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                user.getId(), today, today)) {
            attendanceRepository.save(Attendance.create(user, EntryType.MEMBERSHIP, null));
            return buildResponse(user, maskedName, EntryType.MEMBERSHIP.getLabel(), null,
                    now, true, "입장이 처리되었습니다.", null);
        }

        // 3) 회원권이 없으면 오늘 PT 예약 중 입장 가능 시간대에 있는 것을 찾는다
        int before = settingService.getInt(SettingKey.PT_ENTRY_BEFORE_MINUTES);
        int after = settingService.getInt(SettingKey.PT_ENTRY_AFTER_MINUTES);
        List<PtReservation> todays = ptReservationRepository
                .findByUserIdAndLessonDateBetween(user.getId(), today, today).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .sorted(Comparator.comparing(PtReservation::getStartTime))
                .toList();

        PtReservation matched = null;
        LocalTime nextEntryTime = null;      // 아직 입장 전인 가장 빠른 예약의 입장 가능 시각
        for (PtReservation r : todays) {
            LocalDateTime start = LocalDateTime.of(today, r.getStartTime());
            LocalDateTime open = start.minusMinutes(before);
            LocalDateTime close = start.plusMinutes(after);
            if (!now.isBefore(open) && !now.isAfter(close)) {
                matched = r;
                break;
            }
            if (now.isBefore(open) && nextEntryTime == null) {
                nextEntryTime = open.toLocalTime();
            }
        }

        if (matched != null) {
            Attendance saved = attendanceRepository.save(Attendance.create(user, EntryType.PT, matched));
            return buildResponse(user, maskedName, EntryType.PT.getLabel(), matched.getId(),
                    now, true, "PT 수업 입장이 처리되었습니다.", matched.getPtPass());
        }

        // 4) 입장 불가
        if (nextEntryTime != null) {
            throw new BusinessException(ErrorCode.ENTRY_NOT_ALLOWED,
                    "PT 입장 가능 시각은 " + nextEntryTime.format(HHMM) + "입니다.");
        }
        throw new BusinessException(ErrorCode.ENTRY_NOT_ALLOWED, NO_PASS_MESSAGE);
    }

    // [추가] 입장 완료 화면용 응답: 기본 정보 + 회원권·PT 이용권·이번 달 출석·다음 수업
    private KioskCheckinResponse buildResponse(User user, String maskedName, String entryTypeLabel,
                                               Long reservationId, LocalDateTime checkedInAt,
                                               boolean recorded, String message, PtPass preferredPass) {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        return new KioskCheckinResponse(
                maskedName, entryTypeLabel, reservationId, checkedInAt, !recorded, message,
                user.getName(),
                buildMembershipInfo(user.getId(), today),
                buildPtInfo(user.getId(), today, preferredPass),
                countMonthAttendanceDays(user.getId(), today),
                buildNextLesson(user.getId(), now));
    }

    // [추가] 오늘 유효한 회원권 (여러 개면 종료일이 빠른 것). 없으면 null
    private KioskMembershipInfo buildMembershipInfo(Long userId, LocalDate today) {
        return membershipRepository.findByUserIdOrderByStartDateDescIdDesc(userId).stream()
                .filter(m -> !m.getStartDate().isAfter(today) && !m.getEndDate().isBefore(today))
                .min(Comparator.comparing(Membership::getEndDate))
                .map(m -> new KioskMembershipInfo(
                        m.getProduct().getName(),
                        (int) ChronoUnit.DAYS.between(today, m.getEndDate()),      // 종료 당일은 0
                        (int) ChronoUnit.DAYS.between(m.getStartDate(), m.getEndDate()) + 1,
                        m.getEndDate()))
                .orElse(null);
    }

    // [추가] PT로 입장했으면 그 예약의 이용권, 아니면 오늘 유효한 PT권 중 종료일이 가장 빠른 것. 없으면 null
    private KioskPtInfo buildPtInfo(Long userId, LocalDate today, PtPass preferredPass) {
        PtPass pass = preferredPass != null ? preferredPass
                : ptPassRepository.findByUserIdOrderByStartDateDescIdDesc(userId).stream()
                .filter(p -> !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today))
                .min(Comparator.comparing(PtPass::getEndDate))
                .orElse(null);
        if (pass == null) {
            return null;
        }
        long reserved = ptReservationRepository.countByPtPassIdAndStatusIn(pass.getId(), HOLDING);
        int remaining = pass.getRemainingCount();
        long available = Math.max(0, remaining - reserved);
        return new KioskPtInfo(pass.getProduct().getName(), remaining, reserved, available,
                pass.getProduct().getTotalCount());
    }

    // [추가] 이번 달에 출석한 날 수 (같은 날 여러 번 입장해도 1일)
    private long countMonthAttendanceDays(Long userId, LocalDate today) {
        LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();
        return attendanceRepository
                .findByUserIdAndCheckedInAtGreaterThanEqualAndCheckedInAtLessThan(userId, from, to).stream()
                .map(a -> a.getCheckedInAt().toLocalDate())
                .distinct()
                .count();
    }

    // [추가] 지금 이후 가장 가까운 '예약' 상태 수업 (개인·그룹 모두). 없으면 null
    private KioskNextLesson buildNextLesson(Long userId, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        int rangeDays = Math.max(
                settingService.getInt(SettingKey.BOOKING_RANGE_DAYS),
                settingService.getInt(SettingKey.GROUP_OPEN_MAX_DAYS));

        return ptReservationRepository
                .findByUserIdAndLessonDateBetween(userId, today, today.plusDays(rangeDays)).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .filter(r -> !LocalDateTime.of(r.getLessonDate(), r.getStartTime()).isBefore(now))
                .min(Comparator.comparing((PtReservation r) -> LocalDateTime.of(r.getLessonDate(), r.getStartTime())))
                .map(r -> new KioskNextLesson(
                        r.getLessonDate().format(LESSON_DATE),
                        r.getStartTime().format(HHMM),
                        r.getGroupClass() == null ? "개인 PT" : r.getGroupClass().getName(),
                        r.getTrainer().getName()))
                .orElse(null);
    }

    /** 내 출석 기록 (기간 생략 가능) */
    @Transactional(readOnly = true)
    public AttendancePageResponse getMyAttendances(Long userId, LocalDate from, LocalDate to, String entryType,
                                                   int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 1 이상, size는 1~100 이어야 합니다.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일이 종료일보다 늦을 수 없습니다.");
        }

        // 관리자 출석 검색과 같은 쿼리를 본인 것만 조회 (기간·입장 구분 조건은 없으면 무시)
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "checkedInAt"));
        Page<Attendance> result = attendanceRepository.searchForAdmin(
                userId, parseEntryType(entryType),
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                pageable);

        List<AttendanceItemResponse> items = result.getContent().stream()
                .map(a -> new AttendanceItemResponse(a.getId(), a.getCheckedInAt(),
                        a.getEntryType().getLabel(),
                        a.getReservation() == null ? null : a.getReservation().getId()))
                .toList();

        return new AttendancePageResponse(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /**
     * 관리자 출석 검색. date를 주면 그 하루, 아니면 startDate~endDate (둘 다 없으면 전체 기간).
     * userId·entryType(회원권/PT)으로 거를 수 있고, page는 1부터.
     */
    @Transactional(readOnly = true)
    public AdminAttendanceListResponse searchForAdmin(LocalDate date, LocalDate startDate, LocalDate endDate,
                                                      Long userId, String entryType, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 1 이상, size는 1~100 이어야 합니다.");
        }
        LocalDate from = date != null ? date : startDate;
        LocalDate to = date != null ? date : endDate;
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일이 종료일보다 늦을 수 없습니다.");
        }

        Page<Attendance> result = attendanceRepository.searchForAdmin(
                userId,
                parseEntryType(entryType),
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),     // 종료일 하루 전체 포함
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "checkedInAt")));

        List<AdminAttendanceItemResponse> items = result.getContent().stream()
                .map(a -> new AdminAttendanceItemResponse(a.getId(), a.getUser().getId(),
                        a.getUser().getName(), a.getUser().getPhone(), a.getCheckedInAt(),
                        a.getEntryType().getLabel(),
                        a.getReservation() == null ? null : a.getReservation().getId()))
                .toList();
        return new AdminAttendanceListResponse(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    // 회원권/PT (MEMBERSHIP/PT도 받음). 없거나 전체면 null(조건 없음)
    private EntryType parseEntryType(String value) {
        if (value == null || value.isBlank() || value.equals("전체")) return null;
        try {
            return EntryType.fromJson(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "entryType은 회원권 또는 PT여야 합니다.");
        }
    }

    /** 홍길동 → 홍*동, 이수 → 이* */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}