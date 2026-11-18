package com.fatenexx.backend.attendance.service;

import com.fatenexx.backend.attendance.dto.*;
import com.fatenexx.backend.attendance.entity.Attendance;
import com.fatenexx.backend.attendance.entity.EntryType;
import com.fatenexx.backend.attendance.repository.AttendanceRepository;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.pass.repository.MembershipRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String NO_PASS_MESSAGE = "입장 가능한 이용권이 없습니다. 데스크에 문의해 주세요.";

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
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
            return new KioskCheckinResponse(maskedName, a.getEntryType().name(),
                    a.getReservation() == null ? null : a.getReservation().getId(),
                    a.getCheckedInAt(), false, "이미 입장 처리되었습니다.");
        }

        // 2) 오늘 유효한 회원권이 있으면 회원권 입장 (회원권과 PT 예약이 둘 다 있어도 회원권 우선, 처리 규칙 9번)
        if (membershipRepository.existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                user.getId(), today, today)) {
            attendanceRepository.save(Attendance.create(user, EntryType.MEMBERSHIP, null));
            return new KioskCheckinResponse(maskedName, "MEMBERSHIP", null,
                    now, true, "입장이 처리되었습니다.");
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
            return new KioskCheckinResponse(maskedName, "PT", matched.getId(),
                    now, true, "PT 수업 입장이 처리되었습니다.");
        }

        // 4) 입장 불가
        if (nextEntryTime != null) {
            throw new BusinessException(ErrorCode.ENTRY_NOT_ALLOWED,
                    "PT 입장 가능 시각은 " + nextEntryTime.format(HHMM) + "입니다.");
        }
        throw new BusinessException(ErrorCode.ENTRY_NOT_ALLOWED, NO_PASS_MESSAGE);
    }

    /** 내 출석 기록 (기간 생략 가능) */
    @Transactional(readOnly = true)
    public AttendancePageResponse getMyAttendances(Long userId, LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100 이어야 합니다.");
        }
        LocalDate f = (from == null) ? LocalDate.of(2000, 1, 1) : from;
        LocalDate t = (to == null) ? LocalDate.of(2100, 1, 1) : to;
        if (f.isAfter(t)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일이 종료일보다 늦을 수 없습니다.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkedInAt"));
        Page<Attendance> result = attendanceRepository
                .findByUserIdAndCheckedInAtGreaterThanEqualAndCheckedInAtLessThan(
                        userId, f.atStartOfDay(), t.plusDays(1).atStartOfDay(), pageable);

        List<AttendanceItemResponse> items = result.getContent().stream()
                .map(a -> new AttendanceItemResponse(a.getId(), a.getCheckedInAt(),
                        a.getEntryType().name(),
                        a.getReservation() == null ? null : a.getReservation().getId()))
                .toList();

        return new AttendancePageResponse(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** 관리자: 하루치 입장 기록 (date 생략 시 오늘) */
    @Transactional(readOnly = true)
    public AdminAttendanceListResponse getAttendancesByDate(LocalDate date) {
        LocalDate d = (date == null) ? LocalDate.now(KST) : date;
        List<AdminAttendanceItemResponse> items = attendanceRepository
                .findByDay(d.atStartOfDay(), d.plusDays(1).atStartOfDay()).stream()
                .map(a -> new AdminAttendanceItemResponse(a.getId(), a.getUser().getId(),
                        a.getUser().getName(), a.getUser().getPhone(), a.getCheckedInAt(),
                        a.getEntryType().name(),
                        a.getReservation() == null ? null : a.getReservation().getId()))
                .toList();
        return new AdminAttendanceListResponse(d, items.size(), items);
    }

    /** 홍길동 → 홍*동, 이수 → 이* */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}