package com.fatenexx.backend.pass.entity;

import com.fatenexx.backend.payment.entity.Payment;
import com.fatenexx.backend.product.entity.MembershipProduct;
import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private MembershipProduct product;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;            // 결제 1건 = 회원권 1개

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price", nullable = false)
    private Integer price;              // 구매 당시 가격 (BR-PASS-08)

    // 예: 10/23 시작 3개월권 → 1/22까지
    public static Membership create(User user, MembershipProduct product, Payment payment, LocalDate startDate) {
        Membership m = new Membership();
        m.user = user;
        m.product = product;
        m.payment = payment;
        m.startDate = startDate;
        m.endDate = startDate.plusMonths(product.getMonths()).minusDays(1);
        m.price = product.getPrice();
        return m;
    }
}