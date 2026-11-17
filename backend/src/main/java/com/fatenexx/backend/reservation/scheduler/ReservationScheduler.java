package com.fatenexx.backend.reservation.scheduler;

import com.fatenexx.backend.group.service.GroupClassService;
import com.fatenexx.backend.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;
    private final GroupClassService groupClassService;

    // 매분 0초 실행
    @Scheduled(cron = "0 * * * * *")
    public void run() {
        try {
            int pending = reservationService.markEndedAsPending();
            if (pending > 0) {
                log.info("[스케줄러] 결과대기 전환 {}건", pending);
            }
        } catch (Exception e) {
            log.error("[스케줄러] 결과대기 전환 실패", e);
        }
        try {
            GroupClassService.DecisionResult d = groupClassService.decideDueClasses();
            if (d.confirmed() > 0 || d.closed() > 0) {
                log.info("[스케줄러] 그룹 수업 확정 {}건, 폐강 {}건", d.confirmed(), d.closed());
            }
        } catch (Exception e) {
            log.error("[스케줄러] 그룹 수업 판정 실패", e);
        }
    }
}