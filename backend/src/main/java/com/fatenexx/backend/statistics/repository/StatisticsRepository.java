package com.fatenexx.backend.statistics.repository;

import com.fatenexx.backend.group.entity.GroupClassStatus;
import com.fatenexx.backend.payment.entity.PaymentStatus;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 통계용 조회 전용. 여러 도메인(결제·출석·예약·수업)을 가로지르는 집계 쿼리
 */
@Repository
public class StatisticsRepository {

    @PersistenceContext
    private EntityManager em;

    /** 기간 내 입장 수 (from 이상 ~ to 미만) */
    public long countEntries(LocalDateTime from, LocalDateTime to) {
        return em.createQuery(
                        "select count(a) from Attendance a " +
                                "where a.checkedInAt >= :from and a.checkedInAt < :to", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    /** 오늘 유효한 회원권을 가진 회원 수 */
    public long countActiveMembers(LocalDate today) {
        return em.createQuery(
                        "select count(distinct m.user.id) from Membership m " +
                                "where m.startDate <= :today and m.endDate >= :today", Long.class)
                .setParameter("today", today)
                .getSingleResult();
    }

    /** 특정 날짜의 PT 수업 수 (취소 제외) */
    public long countLessons(LocalDate date, ReservationStatus excluded) {
        return em.createQuery(
                        "select count(r) from PtReservation r " +
                                "where r.lessonDate = :date and r.status <> :excluded", Long.class)
                .setParameter("date", date)
                .setParameter("excluded", excluded)
                .getSingleResult();
    }

    /** 특정 상태의 예약 수 */
    public long countReservationsByStatus(ReservationStatus status) {
        return em.createQuery(
                        "select count(r) from PtReservation r where r.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    /** 마감 임박 수업: 개설 상태이고 참여자(취소 제외)가 최소 인원 미만인 수업 수 */
    public long countClosingSoonClasses(LocalDate today, int minParticipants,
                                        GroupClassStatus open, ReservationStatus canceled) {
        return em.createQuery(
                        "select count(g) from GroupClass g " +
                                "where g.status = :open and g.classDate >= :today " +
                                "and (select count(r) from PtReservation r " +
                                "     where r.groupClass = g and r.status <> :canceled) < :min", Long.class)
                .setParameter("open", open)
                .setParameter("today", today)
                .setParameter("canceled", canceled)
                .setParameter("min", (long) minParticipants)
                .getSingleResult();
    }

    /** 기간 내 성공 결제 합계 (from 이상 ~ to 미만) */
    public long sumRevenue(PaymentStatus status, LocalDateTime from, LocalDateTime to) {
        return em.createQuery(
                        "select coalesce(sum(p.amount), 0) from Payment p " +
                                "where p.status = :status and p.paidAt >= :from and p.paidAt < :to", Long.class)
                .setParameter("status", status)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    /** 월별 매출: [연, 월, 합계, 건수] */
    public List<Object[]> monthlyRevenue(PaymentStatus status, LocalDateTime from, LocalDateTime to) {
        return em.createQuery(
                        "select extract(year from p.paidAt), extract(month from p.paidAt), sum(p.amount), count(p) " +
                                "from Payment p " +
                                "where p.status = :status and p.paidAt >= :from and p.paidAt < :to " +
                                "group by extract(year from p.paidAt), extract(month from p.paidAt) " +
                                "order by extract(year from p.paidAt), extract(month from p.paidAt)", Object[].class)
                .setParameter("status", status)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** 트레이너별 예약 상태 건수: [트레이너id, 이름, 상태, 건수] */
    public List<Object[]> trainerReservationCounts(LocalDate from, LocalDate to) {
        return em.createQuery(
                        "select t.id, t.name, r.status, count(r) from PtReservation r join r.trainer t " +
                                "where r.lessonDate >= :from and r.lessonDate <= :to " +
                                "group by t.id, t.name, r.status " +
                                "order by t.id", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** 시간대별 입장 수: [시, 건수] */
    public List<Object[]> hourlyEntries(LocalDateTime from, LocalDateTime to) {
        return em.createQuery(
                        "select extract(hour from a.checkedInAt), count(a) from Attendance a " +
                                "where a.checkedInAt >= :from and a.checkedInAt < :to " +
                                "group by extract(hour from a.checkedInAt) " +
                                "order by extract(hour from a.checkedInAt)", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}