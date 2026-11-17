package com.fatenexx.backend.trainer.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.trainer.dto.TrainerListResponse;
import com.fatenexx.backend.trainer.dto.TrainerSummaryResponse;
import com.fatenexx.backend.trainer.dto.WorkTimeResponse;
import com.fatenexx.backend.trainer.repository.TrainerScheduleRepository;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerService {

    private final UserRepository userRepository;
    private final TrainerScheduleRepository scheduleRepository;

    // ① 회원용 트레이너 목록 (승인된 트레이너만)
    public TrainerListResponse getTrainers() {
        List<User> trainers = userRepository
                .searchByRole(Role.TRAINER, null, Pageable.unpaged())
                .stream()
                .filter(User::isApproved)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        Map<Long, List<WorkTimeResponse>> workTimes = getWorkTimes(
                trainers.stream().map(User::getId).toList());

        List<TrainerSummaryResponse> items = trainers.stream()
                .map(t -> TrainerSummaryResponse.of(t, workTimes.getOrDefault(t.getId(), List.of())))
                .toList();
        return new TrainerListResponse(items);
    }

    // 트레이너별 근무 시간을 한 번에 꺼내서 트레이너 ID로 묶기
    private Map<Long, List<WorkTimeResponse>> getWorkTimes(List<Long> trainerIds) {
        if (trainerIds.isEmpty()) {
            return Map.of();
        }
        return scheduleRepository.findByTrainerIdInOrderByDayOfWeekAscStartTimeAsc(trainerIds).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTrainer().getId(),
                        Collectors.mapping(WorkTimeResponse::from, Collectors.toList())));
    }
}