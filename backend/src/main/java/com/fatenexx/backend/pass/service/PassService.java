package com.fatenexx.backend.pass.service;

import com.fatenexx.backend.pass.dto.*;
import com.fatenexx.backend.pass.entity.Membership;
import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.pass.repository.MembershipRepository;
import com.fatenexx.backend.pass.repository.PtPassRepository;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import com.fatenexx.backend.reservation.repository.PtReservationRepository;
import com.fatenexx.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PassService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 횟수를 미리 확보하고 있는 예약 상태 (아직 차감은 안 됨)
    private static final List<ReservationStatus> HOLDING =
            List.of(ReservationStatus.RESERVED, ReservationStatus.PENDING_RESULT);

    private final MembershipRepository membershipRepository;
    private final PtPassRepository ptPassRepository;
    private final PtReservationRepository ptReservationRepository;

    /** 내 회원권 + PT권 (회원 본인) */
    @Transactional(readOnly = true)
    public MyPassesResponse getMyPasses(Long userId) {
        LocalDate today = LocalDate.now(KST);

        List<MembershipItemResponse> memberships = membershipRepository
                .findByUserIdOrderByStartDateDescIdDesc(userId).stream()
                .map(m -> new MembershipItemResponse(m.getId(), m.getProduct().getName(),
                        m.getStartDate(), m.getEndDate(), membershipStatus(m, today)))
                .toList();

        List<PtPassItemResponse> ptPasses = ptPassRepository
                .findByUserIdOrderByStartDateDescIdDesc(userId).stream()
                .map(p -> toPtPassItem(p, today))
                .toList();

        return new MyPassesResponse(memberships, ptPasses);
    }

    private PtPassItemResponse toPtPassItem(PtPass p, LocalDate today) {
        long reserved = ptReservationRepository.countByPtPassIdAndStatusIn(p.getId(), HOLDING);
        long available = Math.max(0, p.getRemainingCount() - reserved);
        User trainer = p.getTrainer();

        return new PtPassItemResponse(
                p.getId(),
                p.getPtType().name(),
                p.getProduct().getName(),
                trainer == null ? null : trainer.getId(),
                trainer == null ? null : trainer.getName(),
                p.getStartDate(),
                p.getEndDate(),
                p.getProduct().getTotalCount(),
                p.getRemainingCount(),
                reserved,
                available,
                ptPassStatus(p, today));
    }

    private String membershipStatus(Membership m, LocalDate today) {
        if (m.getEndDate().isBefore(today)) return "EXPIRED";
        if (m.getStartDate().isAfter(today)) return "BEFORE_START";
        return "ACTIVE";
    }

    private String ptPassStatus(PtPass p, LocalDate today) {
        if (p.getEndDate().isBefore(today)) return "EXPIRED";
        if (p.getRemainingCount() <= 0) return "EXHAUSTED";
        if (p.getStartDate().isAfter(today)) return "BEFORE_START";
        return "ACTIVE";
    }

    /** 트레이너의 담당 회원 (이용 기간이 남은 개인 PT권을 가진 회원) */
    @Transactional(readOnly = true)
    public ClientListResponse getMyClients(Long trainerId) {
        LocalDate today = LocalDate.now(KST);

        Map<Long, List<PtPass>> byUser = new LinkedHashMap<>();
        for (PtPass p : ptPassRepository.findValidByTrainer(trainerId, today)) {
            byUser.computeIfAbsent(p.getUser().getId(), k -> new ArrayList<>()).add(p);
        }

        List<ClientResponse> clients = byUser.values().stream().map(list -> {
            User u = list.get(0).getUser();
            List<ClientPassResponse> passes = list.stream()
                    .map(p -> new ClientPassResponse(p.getId(), p.getProduct().getName(),
                            p.getStartDate(), p.getEndDate(),
                            p.getProduct().getTotalCount(), p.getRemainingCount()))
                    .toList();
            return new ClientResponse(u.getId(), u.getName(), u.getPhone(), u.getBirthDate(),
                    u.getGender() == null ? null : u.getGender().name(), passes);
        }).toList();

        return new ClientListResponse(clients);
    }
}