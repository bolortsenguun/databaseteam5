package com.fatenexx.backend.reservation.service;

import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.reservation.repository.PtReservationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// 예약할 때 쓸 PT 이용권 자동 선택 (BR-CNL-08, BR-RSV-06·07)
// 그룹 수업 목록(신청 가능 여부)과 PT 예약 신청에서 같이 씀
@Component
@RequiredArgsConstructor
public class PtPassSelector {

    private static final List<ReservationStatus> HOLDING =
            List.of(ReservationStatus.RESERVED, ReservationStatus.PENDING_RESULT);

    private final PtReservationRepository reservationRepository;

    @PersistenceContext
    private EntityManager em;

    // pass가 있으면 그걸로 예약, 없으면 reason이 이유
    public record Result(PtPass pass, String reason) {
        public boolean ok() {
            return pass != null;
        }
    }

    // trainerId: 개인 PT면 담당 트레이너, 그룹이면 null
    public Result select(Long userId, PtType type, Long trainerId, LocalDate lessonDate) {
        String jpql = "select p from PtPass p where p.user.id = :userId and p.ptType = :type " +
                "and p.startDate <= :date and p.endDate >= :date " +
                (trainerId != null ? "and p.trainer.id = :trainerId " : "") +
                "order by p.endDate, p.id";   // 만료일 빠른 것 → 번호 작은 것
        TypedQuery<PtPass> q = em.createQuery(jpql, PtPass.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .setParameter("date", lessonDate);
        if (trainerId != null) {
            q.setParameter("trainerId", trainerId);
        }
        List<PtPass> valid = q.getResultList();

        if (valid.isEmpty()) {
            return new Result(null, type == PtType.GROUP ? "NO_VALID_GROUP_PASS" : "NO_VALID_PERSONAL_PASS");
        }
        for (PtPass p : valid) {
            long holding = reservationRepository.countByPtPassIdAndStatusIn(p.getId(), HOLDING);
            if (p.getRemainingCount() - holding > 0) {   // 예약 가능 횟수 = 잔여 − 확보 (BR-RSV-06)
                return new Result(p, null);
            }
        }
        return new Result(null, "NO_AVAILABLE_COUNT");
    }
}