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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerScheduleService {

    private final UserRepository userRepository;
    private final TrainerScheduleRepository scheduleRepository;
    private final TrainerDayOffRepository dayOffRepository;

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
}