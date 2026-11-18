package com.fatenexx.backend.pass.repository;

import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PtPassRepository extends JpaRepository<PtPass, Long> {

    @EntityGraph(attributePaths = "product")
    List<PtPass> findByPaymentIn(Collection<Payment> payments);

    // 내 PT권 목록 (최신 시작일 순). 그룹 PT는 트레이너가 없을 수 있다
    @EntityGraph(attributePaths = {"product", "trainer"})
    List<PtPass> findByUserIdOrderByStartDateDescIdDesc(Long userId);

    // 트레이너의 담당 회원 조회용: 이용 기간이 남은 PT권
    @Query("select p from PtPass p join fetch p.user u join fetch p.product " +
            "where p.trainer.id = :trainerId and p.endDate >= :today " +
            "order by u.name, p.endDate")
    List<PtPass> findValidByTrainer(@Param("trainerId") Long trainerId,
                                    @Param("today") LocalDate today);
}