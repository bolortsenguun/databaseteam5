package com.fatenexx.backend.payment.entity;

import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                  // 결제한 회원

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status;       // 성공 / 실패

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    public static Payment create(User user, int amount, PaymentStatus status) {
        Payment p = new Payment();
        p.user = user;
        p.amount = amount;
        p.status = status;
        return p;
    }
}