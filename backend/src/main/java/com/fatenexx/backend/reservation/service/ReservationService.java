package com.fatenexx.backend.reservation.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.group.dto.SlotResponse;
import com.fatenexx.backend.group.entity.GroupClass;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import com.fatenexx.backend.group.repository.GroupClassRepository;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.reservation.dto.IndividualReservationRequest;
import com.fatenexx.backend.reservation.dto.IndividualReservationResponse;
import com.fatenexx.backend.reservation.dto.SlotListResponse;
import com.fatenexx.backend.reservation.entity.PtReservation;
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
import com.fatenexx.backend.reservation.dto.MyReservationListResponse;
import com.fatenexx.backend.reservation.dto.MyReservationResponse;
import java.util.function.Function;
import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.reservation.dto.CancelPreviewResponse;
import com.fatenexx.backend.reservation.dto.ReservationCancelResponse;
import com.fatenexx.backend.reservation.entity.CanceledBy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.fatenexx.backend.reservation.dto.StaffCancelResponse;
import com.fatenexx.backend.reservation.dto.LessonListResponse;
import com.fatenexx.backend.reservation.dto.LessonResponse;
import com.fatenexx.backend.reservation.dto.LessonResultResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fatenexx.backend.reservation.dto.AdminReservationListResponse;
import com.fatenexx.backend.reservation.dto.AdminReservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final List<GroupClassStatus> ACTIVE_CLASS = List.of(GroupClassStatus.OPEN, GroupClassStatus.CONFIRMED);

    private final PtReservationRepository reservationRepository;
    private final GroupClassRepository groupClassRepository;
    private final TrainerScheduleRepository scheduleRepository;
    private final TrainerDayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final SettingService settingService;
    private final PtPassSelector passSelector;
    @PersistenceContext
    private EntityManager em;

    // 하루치 판단 재료를 한 번에 모아둔 상자
    private record DayContext(Long userId, Long trainerId, LocalDate date, int closeHours,
                              List<PtReservation> trainerPts, List<GroupClass> classes,
                              List<PtReservation> mine, PtPassSelector.Result pass) {
    }

    // ① 개인 PT 예약 가능 시간
    public SlotListResponse getAvailableSlots(Long userId, Long trainerId, LocalDate date) {
        checkBookingDate(date, ErrorCode.INVALID_DATE_RANGE);
        User trainer = getApprovedTrainer(trainerId);

        List<SlotResponse> slots = new ArrayList<>();
        if (!dayOffRepository.existsByTrainerIdAndOffDate(trainerId, date)) {   // 휴무일이면 슬롯 없음
            DayContext ctx = loadDay(userId, trainerId, date);
            for (LocalTime start : workingSlots(trainerId, date)) {
                String reason = findReason(ctx, start);
                slots.add(new SlotResponse(start, start.plusHours(1), reason == null, reason));
            }
        }
        return new SlotListResponse(trainerId, trainer.getName(), date, slots);
    }

    // ② 개인 PT 예약 신청
    @Transactional
    public IndividualReservationResponse reserveIndividual(Long userId, IndividualReservationRequest req) {
        LocalDate date = req.lessonDate();
        LocalTime start = req.startTime();
        if (start.getMinute() != 0 || start.getSecond() != 0 || start.getHour() > 22) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "예약 날짜와 시간을 확인해 주세요.");
        }
        checkBookingDate(date, ErrorCode.INVALID_INPUT);
        User trainer = getApprovedTrainer(req.trainerId());

        if (dayOffRepository.existsByTrainerIdAndOffDate(trainer.getId(), date)
                || !workingSlots(trainer.getId(), date).contains(start)) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE);
        }

        DayContext ctx = loadDay(userId, trainer.getId(), date);
        String reason = findReason(ctx, start);
        if (reason != null) {
            throw toError(reason);
        }

        User member = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        PtReservation r = PtReservation.createIndividual(ctx.pass().pass(), member, trainer, date, start);
        save(r);
        return IndividualReservationResponse.from(r);
    }

    // ③ 내 PT 예약 목록
    public MyReservationListResponse getMyReservations(Long userId, String status, String ptType) {
        ReservationStatus statusFilter = parseLabel(status, ReservationStatus.values(), ReservationStatus::getLabel);
        PtType typeFilter = parseLabel(ptType, PtType.values(), PtType::getLabel);
        List<MyReservationResponse> items = reservationRepository.findMine(userId, statusFilter, typeFilter).stream()
                .map(MyReservationResponse::from)
                .toList();
        return new MyReservationListResponse(items);
    }

    // ④ 취소 미리보기 (상태 변경 없음)
    public CancelPreviewResponse previewCancel(Long userId, Long reservationId) {
        PtReservation r = getOwnReservation(userId, reservationId, false, "본인의 예약만 확인할 수 있습니다.");
        CancelCheck c = checkCancel(r);
        return new CancelPreviewResponse(r.getId(), c.canCancel(), c.willDeduct(), c.willDeduct() ? 1 : 0, c.message());
    }

    // ⑤ 회원 취소
    @Transactional
    public ReservationCancelResponse cancelByMember(Long userId, Long reservationId, boolean acceptDeduction) {
        PtReservation r = getOwnReservation(userId, reservationId, true, "본인의 예약만 취소할 수 있습니다.");
        CancelCheck c = checkCancel(r);   // 실제 취소 시점에 다시 계산
        if (!c.canCancel()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
        if (c.willDeduct() && !acceptDeduction) {
            throw new BusinessException(ErrorCode.DEDUCTION_CONFIRMATION_REQUIRED);
        }
        if (c.willDeduct()) {
            deduct(r);
        }
        r.cancel(CanceledBy.MEMBER, null, c.willDeduct());
        return ReservationCancelResponse.from(r);
    }

    // ⑨ 트레이너 취소 (개인 PT만, 차감 없음)
    @Transactional
    public StaffCancelResponse cancelByTrainer(Long trainerId, Long reservationId, String reason) {
        validateCancelReason(reason);
        PtReservation r = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!r.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "담당 트레이너만 예약을 취소할 수 있습니다.");
        }
        if (r.getGroupClass() != null) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_TYPE);
        }
        checkStaffCancelable(r);
        r.cancel(CanceledBy.TRAINER, reason, false);
        return StaffCancelResponse.from(r);
    }

    // ⑪ 관리자 취소 (개인·그룹, 차감 없음)
    @Transactional
    public StaffCancelResponse cancelByAdmin(Long reservationId, String reason) {
        validateCancelReason(reason);
        PtReservation r = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        checkStaffCancelable(r);
        r.cancel(CanceledBy.ADMIN, reason, false);
        return StaffCancelResponse.from(r);
    }

    // ⑦ 트레이너 수업 일정
    public LessonListResponse getLessons(Long trainerId, LocalDate startDate, LocalDate endDate, String status) {
        ReservationStatus statusFilter = parseLabel(status, ReservationStatus.values(), ReservationStatus::getLabel);

        LocalDate start = startDate;
        LocalDate end = endDate;
        if (start == null && end == null) {
            if (statusFilter != ReservationStatus.PENDING_RESULT) {   // 결과대기 + 날짜 없음 → 전체 기간
                start = LocalDate.now();
                end = start;
            }
        } else if (start == null) {
            start = end;
        } else if (end == null) {
            end = start;
        }
        if (start != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
        }

        // 그룹은 수업 ID로, 개인은 예약 ID로 묶기 (조회 순서 = 날짜·시간순 유지)
        Map<String, List<PtReservation>> grouped = new LinkedHashMap<>();
        for (PtReservation r : reservationRepository.findLessons(trainerId, start, end, statusFilter)) {
            String key = r.getGroupClass() != null ? "G" + r.getGroupClass().getId() : "R" + r.getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return new LessonListResponse(grouped.values().stream().map(LessonResponse::of).toList());
    }

    public record ResultOutcome(LessonResultResponse data, String message) {
    }

    // ⑧ 출석·결석 처리
    @Transactional
    public ResultOutcome recordResult(Long trainerId, Long reservationId, String result) {
        ReservationStatus target = "출석".equals(result) ? ReservationStatus.ATTENDED
                : "결석".equals(result) ? ReservationStatus.ABSENT : null;
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출석 또는 결석을 선택해 주세요.");
        }
        PtReservation r = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!r.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "담당 트레이너만 출석·결석을 처리할 수 있습니다.");
        }
        if (r.getStatus() == ReservationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATE);
        }
        if (LocalDateTime.now().isBefore(LocalDateTime.of(r.getLessonDate(), r.getStartTime()))) {
            throw new BusinessException(ErrorCode.LESSON_NOT_STARTED);
        }

        ReservationStatus before = r.getStatus();
        int deducted = 0;
        if (!r.isDeducted()) {   // 처음 처리할 때만 차감
            deduct(r);
            deducted = 1;
        }
        r.markResult(target);

        boolean corrected = (before == ReservationStatus.ATTENDED || before == ReservationStatus.ABSENT) && before != target;
        String message = target.getLabel() + (corrected ? "으로 정정되었습니다." : " 처리가 완료되었습니다.");
        LessonResultResponse data = new LessonResultResponse(r.getId(), r.getStatus().getLabel(), r.isDeducted(),
                deducted, r.getPtPass().getRemainingCount());
        return new ResultOutcome(data, message);
    }

    // ⑩ 관리자 예약 목록
    public AdminReservationListResponse getReservationsForAdmin(Long userId, Long trainerId, String ptType, String status,
                                                                LocalDate startDate, LocalDate endDate, int page, int size) {
        PtType typeFilter = parseLabel(ptType, PtType.values(), PtType::getLabel);
        ReservationStatus statusFilter = parseLabel(status, ReservationStatus.values(), ReservationStatus::getLabel);
        if (page < 1 || size < 1 || (startDate != null && endDate != null && startDate.isAfter(endDate))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
        }
        Page<PtReservation> result = reservationRepository.searchForAdmin(userId, trainerId, typeFilter, statusFilter,
                startDate, endDate, PageRequest.of(page - 1, size));
        return new AdminReservationListResponse(
                result.getContent().stream().map(AdminReservationResponse::from).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    // 스케줄러: 종료된 예약 → 결과대기 (BR-CNL-03)
    @Transactional
    public int markEndedAsPending() {
        LocalDateTime now = LocalDateTime.now();
        List<PtReservation> ended = reservationRepository.findEnded(
                ReservationStatus.RESERVED, now.toLocalDate(), now.toLocalTime());
        ended.forEach(PtReservation::markPendingResult);
        return ended.size();
    }

    private void validateCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.CANCEL_REASON_REQUIRED);
        }
        if (reason.length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "취소 사유는 200자 이하로 입력해 주세요.");
        }
    }

    // 예약 상태이고 수업 시작 전이어야 함 (24시간 규칙 없음)
    private void checkStaffCancelable(PtReservation r) {
        boolean started = !LocalDateTime.now().isBefore(LocalDateTime.of(r.getLessonDate(), r.getStartTime()));
        if (r.getStatus() != ReservationStatus.RESERVED || started) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
    }

    // ===== 취소·차감 공통 =====

    private record CancelCheck(boolean canCancel, boolean willDeduct, String message) {
    }

    // ④·⑤가 같이 쓰는 취소 판단
    private CancelCheck checkCancel(PtReservation r) {
        if (r.getStatus() != ReservationStatus.RESERVED) {
            return new CancelCheck(false, false, "이미 취소되었거나 처리된 예약은 취소할 수 없습니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.of(r.getLessonDate(), r.getStartTime());
        if (!now.isBefore(start)) {
            return new CancelCheck(false, false, "수업 시작 후에는 예약을 취소할 수 없습니다.");
        }
        boolean early = !now.isAfter(start.minusHours(settingService.getInt(SettingKey.CANCEL_DEADLINE_HOURS)));
        boolean justBooked = !now.isAfter(r.getReservedAt().plusMinutes(settingService.getInt(SettingKey.FREE_CANCEL_MINUTES)));
        if (early || justBooked) {
            return new CancelCheck(true, false, "횟수 차감 없이 취소할 수 있습니다.");
        }
        return new CancelCheck(true, true, "지금 취소하면 PT 1회가 차감됩니다.");
    }

    // 본인 예약인지 확인 (없으면 404, 남의 예약이면 403). lock이면 행 잠금
    private PtReservation getOwnReservation(Long userId, Long reservationId, boolean lock, String forbiddenMessage) {
        PtReservation r = (lock ? reservationRepository.findByIdForUpdate(reservationId)
                : reservationRepository.findById(reservationId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!r.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, forbiddenMessage);
        }
        return r;
    }

    // 이용권 행 잠그고 1회 차감 (⑧ 출석·결석에서도 사용)
    private void deduct(PtReservation r) {
        PtPass pass = em.find(PtPass.class, r.getPtPass().getId(), LockModeType.PESSIMISTIC_WRITE);
        pass.deduct();
    }

    // "전체"·null → 필터 없음, 그 외 라벨 → enum, 모르는 값 → 400
    private <E> E parseLabel(String value, E[] values, Function<E, String> label) {
        if (value == null || value.equals("전체")) {
            return null;
        }
        for (E e : values) {
            if (label.apply(e).equals(value)) {
                return e;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
    }

    // ===== 공통 =====

    // 그날 판단에 필요한 데이터를 한 번에 조회
    private DayContext loadDay(Long userId, Long trainerId, LocalDate date) {
        return new DayContext(userId, trainerId, date,
                settingService.getInt(SettingKey.BOOKING_CLOSE_HOURS),
                reservationRepository.findByTrainerIdAndLessonDateAndGroupClassIsNullAndStatusNot(
                        trainerId, date, ReservationStatus.CANCELED),
                groupClassRepository.findByTrainerIdAndClassDateAndStatusIn(trainerId, date, ACTIVE_CLASS),
                reservationRepository.findByUserIdAndLessonDateBetween(userId, date, date),
                passSelector.select(userId, PtType.PERSONAL, trainerId, date));
    }

    // 예약 못 하는 이유 (우선순위 순), 가능하면 null
    private String findReason(DayContext ctx, LocalTime start) {
        LocalTime end = start.plusHours(1);

        boolean mine = ctx.mine().stream().anyMatch(r -> r.getStatus() != ReservationStatus.CANCELED
                && isSameIndividualSlot(r, ctx.trainerId(), start));
        if (mine) return "MY_RESERVATION";

        if (ctx.classes().stream().anyMatch(g -> g.getStartTime().isBefore(end) && g.getEndTime().isAfter(start))) {
            return "GROUP_CLASS";
        }
        if (ctx.trainerPts().stream().anyMatch(r -> r.getStartTime().isBefore(end) && r.getEndTime().isAfter(start))) {
            return "SLOT_UNAVAILABLE";
        }

        LocalDateTime closeAt = LocalDateTime.of(ctx.date(), start).minusHours(ctx.closeHours());
        if (!LocalDateTime.now().isBefore(closeAt)) return "BOOKING_CLOSED";

        if (ctx.mine().stream().anyMatch(r -> r.getStatus() != ReservationStatus.CANCELED)) {
            return "DAILY_LIMIT_REACHED";
        }

        boolean deductedCancel = ctx.mine().stream().anyMatch(r -> r.getStatus() == ReservationStatus.CANCELED
                && r.isDeducted() && isSameIndividualSlot(r, ctx.trainerId(), start));
        if (deductedCancel) return "REBOOK_NOT_ALLOWED";

        return ctx.pass().reason();   // 이용권 문제면 사유, 괜찮으면 null
    }

    private boolean isSameIndividualSlot(PtReservation r, Long trainerId, LocalTime start) {
        return r.getGroupClass() == null
                && r.getTrainer().getId().equals(trainerId)
                && r.getStartTime().equals(start);
    }

    // 화면용 사유 → 에러 (자리 관련 3개는 SLOT_UNAVAILABLE 하나로)
    private BusinessException toError(String reason) {
        return switch (reason) {
            case "MY_RESERVATION", "GROUP_CLASS", "SLOT_UNAVAILABLE" -> new BusinessException(ErrorCode.SLOT_UNAVAILABLE);
            default -> new BusinessException(ErrorCode.valueOf(reason));
        };
    }

    // 저장 + 동시 요청으로 DB 제약에 걸리면 409로
    private void save(PtReservation r) {
        try {
            reservationRepository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            throw new BusinessException(msg.contains("uq_resv_user_day")
                    ? ErrorCode.DAILY_LIMIT_REACHED : ErrorCode.SLOT_UNAVAILABLE);
        }
    }

    // 오늘 ~ 14일 후 (BOOKING_RANGE_DAYS)
    private void checkBookingDate(LocalDate date, ErrorCode error) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(settingService.getInt(SettingKey.BOOKING_RANGE_DAYS)))) {
            throw new BusinessException(error, error == ErrorCode.INVALID_INPUT ? "예약 날짜와 시간을 확인해 주세요." : error.getMessage());
        }
    }

    private User getApprovedTrainer(Long trainerId) {
        return userRepository.findByIdAndRole(trainerId, Role.TRAINER)
                .filter(User::isApproved)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));
    }

    // 근무 시간 안의 정시 시작 시간들
    private List<LocalTime> workingSlots(Long trainerId, LocalDate date) {
        List<LocalTime> result = new ArrayList<>();
        for (TrainerSchedule s : scheduleRepository.findByTrainerIdAndDayOfWeekOrderByStartTimeAsc(
                trainerId, date.getDayOfWeek().getValue())) {
            int first = s.getStartTime().getMinute() == 0 ? s.getStartTime().getHour() : s.getStartTime().getHour() + 1;
            for (int h = first; h <= s.getEndTime().getHour() - 1; h++) {
                result.add(LocalTime.of(h, 0));
            }
        }
        return result;
    }
}