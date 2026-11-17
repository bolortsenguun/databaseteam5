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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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