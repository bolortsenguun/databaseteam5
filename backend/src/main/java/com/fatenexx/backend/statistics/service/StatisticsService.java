package com.fatenexx.backend.statistics.service;

import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import com.fatenexx.backend.payment.entity.PaymentStatus;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.setting.SettingKey;
import com.fatenexx.backend.setting.service.SettingService;
import com.fatenexx.backend.statistics.dto.*;
import com.fatenexx.backend.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StatisticsRepository statisticsRepository;
    private final SettingService settingService;

    /** 관리자 현황 대시보드 */
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        int minParticipants = settingService.getInt(SettingKey.GROUP_MIN_PARTICIPANTS);

        return new DashboardResponse(
                today,
                statisticsRepository.countEntries(start, end),
                statisticsRepository.countActiveMembers(today),
                statisticsRepository.countLessons(today, ReservationStatus.CANCELED),
                statisticsRepository.countReservationsByStatus(ReservationStatus.PENDING_RESULT),
                statisticsRepository.countClosingSoonClasses(today, minParticipants,
                        GroupClassStatus.OPEN, ReservationStatus.CANCELED),
                statisticsRepository.sumRevenue(PaymentStatus.SUCCESS, start, end));
    }

    /** 관리자 통계 (기간 생략 시 이번 달 1일 ~ 오늘) */
    public StatisticsResponse getStatistics(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(KST);
        LocalDate f = (from == null) ? today.withDayOfMonth(1) : from;
        LocalDate t = (to == null) ? today : to;
        if (f.isAfter(t)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일이 종료일보다 늦을 수 없습니다.");
        }
        LocalDateTime start = f.atStartOfDay();
        LocalDateTime end = t.plusDays(1).atStartOfDay();

        long totalRevenue = statisticsRepository.sumRevenue(PaymentStatus.SUCCESS, start, end);

        List<MonthlyRevenueItem> monthly = new ArrayList<>();
        for (Object[] row : statisticsRepository.monthlyRevenue(PaymentStatus.SUCCESS, start, end)) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            monthly.add(new MonthlyRevenueItem(String.format("%04d-%02d", year, month),
                    ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }

        return new StatisticsResponse(f, t, totalRevenue, monthly,
                buildTrainerPerformance(f, t), buildHourlyEntries(start, end));
    }

    /** (트레이너, 상태, 건수) 행들을 트레이너별 한 줄로 합친다 */
    private List<TrainerPerformanceItem> buildTrainerPerformance(LocalDate from, LocalDate to) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, long[]> counts = new LinkedHashMap<>();   // [출석, 결석, 취소, 예정]

        for (Object[] row : statisticsRepository.trainerReservationCounts(from, to)) {
            Long trainerId = ((Number) row[0]).longValue();
            names.put(trainerId, (String) row[1]);
            ReservationStatus status = (ReservationStatus) row[2];
            long count = ((Number) row[3]).longValue();

            long[] arr = counts.computeIfAbsent(trainerId, k -> new long[4]);
            switch (status) {
                case ATTENDED -> arr[0] += count;
                case ABSENT -> arr[1] += count;
                case CANCELED -> arr[2] += count;
                default -> arr[3] += count;      // 예약, 결과대기
            }
        }

        List<TrainerPerformanceItem> result = new ArrayList<>();
        counts.forEach((id, arr) ->
                result.add(new TrainerPerformanceItem(id, names.get(id), arr[0], arr[1], arr[2], arr[3])));
        return result;
    }

    /** 0~23시를 모두 채운다 (입장이 없는 시간은 0) */
    private List<HourlyEntryItem> buildHourlyEntries(LocalDateTime start, LocalDateTime end) {
        long[] byHour = new long[24];
        for (Object[] row : statisticsRepository.hourlyEntries(start, end)) {
            int hour = ((Number) row[0]).intValue();
            if (hour >= 0 && hour < 24) {
                byHour[hour] = ((Number) row[1]).longValue();
            }
        }
        List<HourlyEntryItem> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new HourlyEntryItem(h, byHour[h]));
        }
        return result;
    }
}