package com.fatenexx.backend.group.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.group.dto.AvailableSlotsResponse;
import com.fatenexx.backend.group.dto.GroupClassCreateRequest;
import com.fatenexx.backend.group.dto.GroupClassResponse;
import com.fatenexx.backend.group.dto.SlotResponse;
import com.fatenexx.backend.group.entity.GroupClass;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import com.fatenexx.backend.group.repository.GroupClassRepository;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.reservation.repository.PtReservationRepository;
import com.fatenexx.backend.setting.SettingKey;
import com.fatenexx.backend.setting.service.SettingService;
import com.fatenexx.backend.trainer.entity.TrainerSchedule;
import com.fatenexx.backend.trainer.repository.TrainerDayOffRepository;
import com.fatenexx.backend.trainer.repository.TrainerScheduleRepository;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fatenexx.backend.group.dto.ParticipantResponse;
import com.fatenexx.backend.group.dto.TrainerGroupClassListResponse;
import com.fatenexx.backend.group.dto.TrainerGroupClassResponse;
import com.fatenexx.backend.group.dto.GroupClassCancelRequest;
import com.fatenexx.backend.group.dto.GroupClassCancelResponse;
import com.fatenexx.backend.group.dto.GroupClassUpdateRequest;
import com.fatenexx.backend.reservation.entity.CanceledBy;
import com.fatenexx.backend.reservation.entity.PtReservation;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.fatenexx.backend.group.dto.MemberGroupClassListResponse;
import com.fatenexx.backend.group.dto.MemberGroupClassResponse;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.reservation.service.PtPassSelector;
import com.fatenexx.backend.reservation.dto.GroupReservationResponse;

import java.util.HashMap;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupClassService {

    private static final List<GroupClassStatus> ACTIVE = List.of(GroupClassStatus.OPEN, GroupClassStatus.CONFIRMED);

    private final GroupClassRepository groupClassRepository;
    private final PtReservationRepository reservationRepository;
    private final TrainerScheduleRepository scheduleRepository;
    private final TrainerDayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final SettingService settingService;
    private final PtPassSelector passSelector;

    // ① 회원용 그룹 수업 목록 (오늘 ~ 14일)
    public MemberGroupClassListResponse getClassesForMember(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(settingService.getInt(SettingKey.BOOKING_RANGE_DAYS));
        LocalDate start = startDate != null ? startDate : today;
        LocalDate end = endDate != null ? endDate : maxDate;
        if (start.isBefore(today) || end.isAfter(maxDate) || start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<GroupClass> classes = groupClassRepository.findForMember(ACTIVE, start, end);
        Map<Long, Long> counts = countParticipants(classes.stream().map(GroupClass::getId).toList());
        List<PtReservation> mine = reservationRepository.findByUserIdAndLessonDateBetween(userId, start, end);

        LocalDateTime now = LocalDateTime.now();
        int closeHours = settingService.getInt(SettingKey.BOOKING_CLOSE_HOURS);
        Map<LocalDate, String> passReasonByDate = new HashMap<>();   // 이용권 판단은 날짜별로 한 번만

        List<MemberGroupClassResponse> items = new ArrayList<>();
        for (GroupClass g : classes) {
            int current = counts.getOrDefault(g.getId(), 0L).intValue();
            String reason = findReason(g, current, mine, now, closeHours, userId, passReasonByDate);
            items.add(MemberGroupClassResponse.of(g, current, reason));
        }
        return new MemberGroupClassListResponse(items);
    }

    // 신청 못 하는 이유 (우선순위 순), 신청 가능하면 null
    private String findReason(GroupClass g, int current, List<PtReservation> mine, LocalDateTime now,
                              int closeHours, Long userId, Map<LocalDate, String> passReasonByDate) {
        boolean alreadyReserved = mine.stream().anyMatch(r -> isSameClass(r, g) && r.getStatus() != ReservationStatus.CANCELED);
        if (alreadyReserved) return "ALREADY_RESERVED";

        if (current >= g.getCapacity()) return "CLASS_FULL";

        LocalDateTime closeAt = LocalDateTime.of(g.getClassDate(), g.getStartTime()).minusHours(closeHours);
        if (!now.isBefore(closeAt)) return "BOOKING_CLOSED";

        boolean sameDay = mine.stream().anyMatch(r -> r.getLessonDate().equals(g.getClassDate())
                && r.getStatus() != ReservationStatus.CANCELED);
        if (sameDay) return "DAILY_LIMIT_REACHED";

        boolean deductedCancel = mine.stream().anyMatch(r -> isSameClass(r, g)
                && r.getStatus() == ReservationStatus.CANCELED && r.isDeducted());
        if (deductedCancel) return "REBOOK_NOT_ALLOWED";

        return passReasonByDate.computeIfAbsent(g.getClassDate(),
                d -> passSelector.select(userId, PtType.GROUP, null, d).reason());
    }

    private boolean isSameClass(PtReservation r, GroupClass g) {
        return r.getGroupClass() != null && r.getGroupClass().getId().equals(g.getId());
    }

    private Map<Long, Long> countParticipants(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return reservationRepository.countByGroupClassIds(classIds, ReservationStatus.CANCELED).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    // 그룹 신청 때만 명세 문구가 다른 에러
    private static final Map<String, String> APPLY_MESSAGES = Map.of(
            "BOOKING_CLOSED", "예약 가능 시간이 지났습니다.",
            "NO_AVAILABLE_COUNT", "예약 가능한 그룹 PT 횟수가 부족합니다.",
            "REBOOK_NOT_ALLOWED", "이미 차감 취소한 그룹 수업은 다시 신청할 수 없습니다.");

    // PT Reservation ⑥ 그룹 PT 수업 신청
    @Transactional
    public GroupReservationResponse apply(Long userId, Long classId) {
        GroupClass g = groupClassRepository.findByIdForUpdate(classId)   // 수업 행 잠금
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_CLASS_NOT_FOUND));

        // 개설·확정이 아니거나 14일 범위 밖이면 회원에게 안 보이는 수업 → 404
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(settingService.getInt(SettingKey.BOOKING_RANGE_DAYS));
        if (!ACTIVE.contains(g.getStatus()) || g.getClassDate().isBefore(today) || g.getClassDate().isAfter(maxDate)) {
            throw new BusinessException(ErrorCode.GROUP_CLASS_NOT_FOUND);
        }

        int current = (int) reservationRepository.countByGroupClassIdAndStatusNot(classId, ReservationStatus.CANCELED);
        List<PtReservation> mine = reservationRepository.findByUserIdAndLessonDateBetween(
                userId, g.getClassDate(), g.getClassDate());

        String reason = findReason(g, current, mine, LocalDateTime.now(),
                settingService.getInt(SettingKey.BOOKING_CLOSE_HOURS), userId, new HashMap<>());
        if (reason != null) {
            ErrorCode code = ErrorCode.valueOf(reason);
            throw new BusinessException(code, APPLY_MESSAGES.getOrDefault(reason, code.getMessage()));
        }

        PtPassSelector.Result pass = passSelector.select(userId, PtType.GROUP, null, g.getClassDate());
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        PtReservation r = PtReservation.createGroup(pass.pass(), member, g);
        try {
            reservationRepository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            throw new BusinessException(msg.contains("uq_resv_user_day")
                    ? ErrorCode.DAILY_LIMIT_REACHED : ErrorCode.ALREADY_RESERVED);
        }
        return GroupReservationResponse.of(r, current + 1);
    }
    public record DecisionResult(int confirmed, int closed) {
    }

    // 스케줄러: 시작 24시간 전이 된 개설 수업 확정·폐강 (BR-GRP-13)
    @Transactional
    public DecisionResult decideDueClasses() {
        LocalDateTime limit = LocalDateTime.now().plusHours(settingService.getInt(SettingKey.GROUP_CONFIRM_HOURS));
        int min = settingService.getInt(SettingKey.GROUP_MIN_PARTICIPANTS);
        int confirmed = 0;
        int closed = 0;
        for (GroupClass g : groupClassRepository.findDueForDecision(
                GroupClassStatus.OPEN, limit.toLocalDate(), limit.toLocalTime())) {
            long count = reservationRepository.countByGroupClassIdAndStatusNot(g.getId(), ReservationStatus.CANCELED);
            if (count >= min) {
                g.confirm();
                confirmed++;
            } else {
                g.close();
                reservationRepository.findByGroupClassIdAndStatus(g.getId(), ReservationStatus.RESERVED)
                        .forEach(r -> r.cancel(CanceledBy.CLASS_CLOSED, null, false));   // 차감 없음
                closed++;
            }
        }
        return new DecisionResult(confirmed, closed);
    }


    // ② 트레이너 내 그룹 수업 목록
    public TrainerGroupClassListResponse getMyClasses(Long trainerId, String status,
                                                      LocalDate startDate, LocalDate endDate) {
        GroupClassStatus filter = parseStatus(status);
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(settingService.getInt(SettingKey.GROUP_OPEN_MAX_DAYS));
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
        }

        List<GroupClass> classes = groupClassRepository
                .findByTrainerIdAndClassDateBetweenOrderByClassDateAscStartTimeAsc(trainerId, start, end)
                .stream()
                .filter(g -> filter == null || g.getStatus() == filter)
                .toList();

        Map<Long, List<ParticipantResponse>> participants = getParticipants(
                classes.stream().map(GroupClass::getId).toList());

        List<TrainerGroupClassResponse> items = classes.stream()
                .map(g -> TrainerGroupClassResponse.of(g, participants.getOrDefault(g.getId(), List.of())))
                .toList();
        return new TrainerGroupClassListResponse(items);
    }

    // 수업별 참여자 (취소 제외)
    private Map<Long, List<ParticipantResponse>> getParticipants(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return reservationRepository.findWithUserByGroupClassIdIn(classIds, ReservationStatus.CANCELED).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getGroupClass().getId(),
                        Collectors.mapping(ParticipantResponse::from, Collectors.toList())));
    }

    // "전체"·null → 필터 없음, 그 외 라벨 → 상태, 모르는 값 → 400
    private GroupClassStatus parseStatus(String value) {
        if (value == null || value.equals("전체")) {
            return null;
        }
        for (GroupClassStatus s : GroupClassStatus.values()) {
            if (s.getLabel().equals(value)) {
                return s;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
    }

    // ③ 그룹 수업 개설 가능 시간
    public AvailableSlotsResponse getAvailableSlots(Long trainerId, LocalDate classDate) {
        checkDateInRange(classDate);
        boolean dayOff = dayOffRepository.existsByTrainerIdAndOffDate(trainerId, classDate);

        List<SlotResponse> slots = new ArrayList<>();
        List<TrainerSchedule> schedules = scheduleRepository
                .findByTrainerIdAndDayOfWeekOrderByStartTimeAsc(trainerId, classDate.getDayOfWeek().getValue());
        for (TrainerSchedule s : schedules) {
            // 근무 시간 안의 정시 슬롯만 (09:30~18:00 → 10:00 ~ 17:00 시작)
            int firstHour = s.getStartTime().getMinute() == 0 ? s.getStartTime().getHour() : s.getStartTime().getHour() + 1;
            int lastHour = s.getEndTime().getHour() - 1;
            for (int h = firstHour; h <= lastHour; h++) {
                LocalTime start = LocalTime.of(h, 0);
                String reason;
                if (dayOff) {
                    reason = "DAY_OFF";
                } else if (isTooSoon(classDate, start)) {
                    reason = "OUTSIDE_OPENING_RANGE";
                } else {
                    reason = findConflict(trainerId, classDate, start, null);
                }
                slots.add(new SlotResponse(start, start.plusHours(1), reason == null, reason));
            }
        }
        return new AvailableSlotsResponse(classDate, slots);
    }

    // ④ 그룹 수업 개설
    @Transactional
    public GroupClassResponse create(Long trainerId, GroupClassCreateRequest req) {
        validateCapacity(req.capacity());
        validateStartTime(req.startTime());
        checkAvailable(trainerId, req.classDate(), req.startTime(), null);

        User trainer = userRepository.findByIdAndRole(trainerId, Role.TRAINER)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));
        GroupClass g = GroupClass.create(trainer, req.name(), blankToNull(req.description()),
                req.classDate(), req.startTime(), req.capacity());
        try {
            groupClassRepository.saveAndFlush(g);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 시간 개설 요청 → DB 제약(uq_gclass_trainer_slot)이 막음
            throw new BusinessException(ErrorCode.CLASS_TIME_CONFLICT);
        }
        return GroupClassResponse.of(g, 0);
    }

    // ⑤ 그룹 수업 수정 (신청자 0명일 때만)
    @Transactional
    public GroupClassResponse update(Long trainerId, Long classId, GroupClassUpdateRequest req) {
        if (req.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수정할 항목을 하나 이상 입력해 주세요.");
        }
        GroupClass g = getOwnClass(trainerId, classId);
        if (g.getStatus() != GroupClassStatus.OPEN || hasStarted(g)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "개설 상태이고 시작 전인 수업만 수정할 수 있습니다.");
        }
        if (reservationRepository.countByGroupClassIdAndStatusNot(classId, ReservationStatus.CANCELED) > 0) {
            throw new BusinessException(ErrorCode.CLASS_HAS_PARTICIPANTS);
        }

        String name = req.name() != null ? req.name() : g.getName();
        if (name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수업명을 입력해 주세요.");
        }
        String description = req.description() != null ? blankToNull(req.description()) : g.getDescription();
        LocalDate date = req.classDate() != null ? req.classDate() : g.getClassDate();
        LocalTime start = req.startTime() != null ? req.startTime() : g.getStartTime();
        int capacity = req.capacity() != null ? req.capacity() : g.getCapacity();

        validateCapacity(capacity);
        if (req.classDate() != null || req.startTime() != null) {
            validateStartTime(start);
            checkAvailable(trainerId, date, start, classId);   // 자기 자신은 겹침 검사에서 빼기
        }

        g.update(name, description, date, start, capacity);
        try {
            groupClassRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CLASS_TIME_CONFLICT);
        }
        return GroupClassResponse.of(g, 0);
    }

    // ⑥ 그룹 수업 삭제 (예약 기록이 한 번도 없을 때만)
    @Transactional
    public void delete(Long trainerId, Long classId) {
        GroupClass g = getOwnClass(trainerId, classId);
        if (hasStarted(g)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작 전인 수업만 삭제할 수 있습니다.");
        }
        if (reservationRepository.existsByGroupClassId(classId)) {
            throw new BusinessException(ErrorCode.CLASS_HAS_RESERVATIONS);
        }
        groupClassRepository.delete(g);
    }

    // ⑦ 그룹 수업 취소 (트레이너 사정, 신청자 전원 차감 없이 취소)
    @Transactional
    public GroupClassCancelResponse cancel(Long trainerId, Long classId, GroupClassCancelRequest req) {
        String reason = req == null ? null : req.cancelReason();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.CANCEL_REASON_REQUIRED);
        }
        if (reason.length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "취소 사유는 200자 이하로 입력해 주세요.");
        }
        GroupClass g = getOwnClass(trainerId, classId);
        if (!ACTIVE.contains(g.getStatus()) || hasStarted(g)) {
            throw new BusinessException(ErrorCode.CLASS_NOT_CANCELABLE);
        }

        g.cancel(reason);
        List<PtReservation> active = reservationRepository.findByGroupClassIdAndStatus(classId, ReservationStatus.RESERVED);
        active.forEach(r -> r.cancel(CanceledBy.TRAINER, reason, false));   // 차감 없음 (BR-GRP-09)

        return new GroupClassCancelResponse(classId, g.getStatus().getLabel(), reason, g.getCanceledAt(), active.size());
    }

    // 내 수업인지 확인 (없으면 404, 남의 수업이면 403)
    private GroupClass getOwnClass(Long trainerId, Long classId) {
        GroupClass g = groupClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_CLASS_NOT_FOUND));
        if (!g.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 개설한 그룹 수업만 변경할 수 있습니다.");
        }
        return g;
    }

    private boolean hasStarted(GroupClass g) {
        return !LocalDateTime.of(g.getClassDate(), g.getStartTime()).isAfter(LocalDateTime.now());
    }

    // ===== 공통 검사 =====

    // 개설 가능한지 전부 검사 (④ 개설, ⑤ 수정에서 사용)
    private void checkAvailable(Long trainerId, LocalDate date, LocalTime start, Long excludeClassId) {
        checkDateInRange(date);
        if (isTooSoon(date, start)) {
            throw new BusinessException(ErrorCode.INVALID_CLASS_DATE,
                    "수업 시작 " + settingService.getInt(SettingKey.GROUP_OPEN_MIN_HOURS) + "시간 전까지만 개설할 수 있습니다.");
        }
        if (dayOffRepository.existsByTrainerIdAndOffDate(trainerId, date)) {
            throw new BusinessException(ErrorCode.TRAINER_UNAVAILABLE, "휴무일에는 그룹 수업을 개설할 수 없습니다.");
        }
        if (!isWithinWorkingHours(trainerId, date, start)) {
            throw new BusinessException(ErrorCode.TRAINER_UNAVAILABLE, "근무 시간 안에서만 개설할 수 있습니다.");
        }
        if (findConflict(trainerId, date, start, excludeClassId) != null) {
            throw new BusinessException(ErrorCode.CLASS_TIME_CONFLICT);
        }
    }

    // 오늘 ~ 30일 후 (GROUP_OPEN_MAX_DAYS)
    private void checkDateInRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate max = today.plusDays(settingService.getInt(SettingKey.GROUP_OPEN_MAX_DAYS));
        if (date.isBefore(today) || date.isAfter(max)) {
            throw new BusinessException(ErrorCode.INVALID_CLASS_DATE);
        }
    }

    // 수업 시작이 지금 + 48시간(GROUP_OPEN_MIN_HOURS)보다 이른지
    private boolean isTooSoon(LocalDate date, LocalTime start) {
        LocalDateTime limit = LocalDateTime.now().plusHours(settingService.getInt(SettingKey.GROUP_OPEN_MIN_HOURS));
        return LocalDateTime.of(date, start).isBefore(limit);
    }

    // 1시간 수업이 그 요일 근무 시간 안에 들어가는지
    private boolean isWithinWorkingHours(Long trainerId, LocalDate date, LocalTime start) {
        LocalTime end = start.plusHours(1);
        return scheduleRepository
                .findByTrainerIdAndDayOfWeekOrderByStartTimeAsc(trainerId, date.getDayOfWeek().getValue())
                .stream()
                .anyMatch(s -> !start.isBefore(s.getStartTime()) && !end.isAfter(s.getEndTime()));
    }

    // 겹치는 그룹 수업·개인 PT가 있으면 사유, 없으면 null
    private String findConflict(Long trainerId, LocalDate date, LocalTime start, Long excludeClassId) {
        LocalTime end = start.plusHours(1);

        boolean groupConflict = groupClassRepository.findByTrainerIdAndClassDateAndStatusIn(trainerId, date, ACTIVE)
                .stream()
                .filter(g -> !g.getId().equals(excludeClassId))
                .anyMatch(g -> g.getStartTime().isBefore(end) && g.getEndTime().isAfter(start));
        if (groupConflict) {
            return "GROUP_CLASS_CONFLICT";
        }

        boolean ptConflict = reservationRepository
                .findByTrainerIdAndLessonDateAndGroupClassIsNullAndStatusNot(trainerId, date, ReservationStatus.CANCELED)
                .stream()
                .anyMatch(r -> r.getStartTime().isBefore(end) && r.getEndTime().isAfter(start));
        return ptConflict ? "PERSONAL_PT_CONFLICT" : null;
    }

    // 정원 4·5·6
    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 4 || capacity > 6) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "정원은 4, 5, 6명 중에서 선택해 주세요.");
        }
    }

    // 정시만, 23시 시작은 불가 (끝나는 시간이 다음 날이 되어서)
    private void validateStartTime(LocalTime start) {
        if (start.getMinute() != 0 || start.getSecond() != 0 || start.getHour() > 22) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작 시간은 정시(22시까지)만 선택할 수 있습니다.");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}