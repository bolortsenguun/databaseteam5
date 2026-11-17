package com.fatenexx.backend.trainer.service;

import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.trainer.dto.DayOffResponse;
import com.fatenexx.backend.trainer.dto.ScheduleResponse;
import com.fatenexx.backend.trainer.dto.TrainerScheduleResponse;
import com.fatenexx.backend.trainer.repository.TrainerDayOffRepository;
import com.fatenexx.backend.trainer.repository.TrainerScheduleRepository;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fatenexx.backend.trainer.dto.ScheduleCreateRequest;
import com.fatenexx.backend.trainer.dto.ScheduleCreateResponse;
import com.fatenexx.backend.trainer.entity.TrainerSchedule;
import java.util.List;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import com.fatenexx.backend.group.repository.GroupClassRepository;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.reservation.repository.PtReservationRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerScheduleService {

    private final UserRepository userRepository;
    private final TrainerScheduleRepository scheduleRepository;
    private final TrainerDayOffRepository dayOffRepository;
    private final PtReservationRepository reservationRepository;
    private final GroupClassRepository groupClassRepository;

    // ⑥ 근무 일정 조회
    public TrainerScheduleResponse getSchedules(LoginUser loginUser, Long trainerId) {
        checkAccess(loginUser, trainerId);
        getTrainer(trainerId);

        List<ScheduleResponse> schedules = scheduleRepository
                .findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(trainerId)
                .stream().map(ScheduleResponse::from).toList();

        List<DayOffResponse> daysOff = dayOffRepository
                .findByTrainerIdOrderByOffDateAsc(trainerId)
                .stream().map(DayOffResponse::from).toList();

        return new TrainerScheduleResponse(trainerId, schedules, daysOff);
    }

    // ⑦ 근무 일정 추가
    @Transactional
    public ScheduleCreateResponse addSchedule(LoginUser loginUser, Long trainerId, ScheduleCreateRequest request) {
        checkAccess(loginUser, trainerId);
        User trainer = getApprovedTrainer(trainerId);

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료 시간은 시작 시간보다 늦어야 합니다.");
        }

        boolean overlaps = scheduleRepository
                .existsByTrainerIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                        trainerId, request.dayOfWeek(), request.endTime(), request.startTime());
        if (overlaps) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT);
        }

        TrainerSchedule saved = scheduleRepository.save(
                TrainerSchedule.create(trainer, request.dayOfWeek(), request.startTime(), request.endTime()));
        return ScheduleCreateResponse.from(saved, trainerId);
    }

    // ⑧ 근무 일정 삭제
    @Transactional
    public void deleteSchedule(LoginUser loginUser, Long trainerId, Long scheduleId) {
        checkAccess(loginUser, trainerId);

        TrainerSchedule schedule = scheduleRepository.findById(scheduleId)
                .filter(s -> s.getTrainer().getId().equals(trainerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (hasBookingsInSchedule(trainerId, schedule)) {
            throw new BusinessException(ErrorCode.SCHEDULE_HAS_BOOKINGS);
        }
        scheduleRepository.delete(schedule);
    }

    // 트레이너는 본인만, 관리자는 누구나
    private void checkAccess(LoginUser loginUser, Long trainerId) {
        if (loginUser.role() == Role.TRAINER && !loginUser.userId().equals(trainerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 트레이너의 일정에 접근할 권한이 없습니다.");
        }
    }

    // 트레이너 계정인지 확인
    private User getTrainer(Long trainerId) {
        return userRepository.findByIdAndRole(trainerId, Role.TRAINER)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));
    }

    // 승인된 트레이너인지 확인 (승인대기면 404)
    private User getApprovedTrainer(Long trainerId) {
        User trainer = getTrainer(trainerId);
        if (!trainer.isApproved()) {
            throw new BusinessException(ErrorCode.TRAINER_NOT_FOUND);
        }
        return trainer;
    }
    // 이 근무 시간에 앞으로 잡힌 예약·그룹 수업이 있는지 (BR-TRN-03)
    private boolean hasBookingsInSchedule(Long trainerId, TrainerSchedule schedule) {
        LocalDate today = LocalDate.now();

        boolean hasReservation = reservationRepository
                .findByTrainerIdAndStatusAndLessonDateGreaterThanEqual(trainerId, ReservationStatus.RESERVED, today)
                .stream()
                .anyMatch(r -> isInSchedule(schedule, r.getLessonDate(), r.getStartTime(), r.getEndTime()));
        if (hasReservation) {
            return true;
        }

        return groupClassRepository
                .findByTrainerIdAndStatusInAndClassDateGreaterThanEqual(
                        trainerId, List.of(GroupClassStatus.OPEN, GroupClassStatus.CONFIRMED), today)
                .stream()
                .anyMatch(g -> isInSchedule(schedule, g.getClassDate(), g.getStartTime(), g.getEndTime()));
    }

    // 그 날짜가 근무 요일이고, 시간이 근무 시간과 겹치는지
    private boolean isInSchedule(TrainerSchedule s, LocalDate date, LocalTime start, LocalTime end) {
        return date.getDayOfWeek().getValue() == s.getDayOfWeek()
                && start.isBefore(s.getEndTime())
                && end.isAfter(s.getStartTime());
    }
}