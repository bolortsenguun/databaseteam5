package com.fatenexx.backend.pass.entity;

import com.fatenexx.backend.payment.entity.Payment;
import com.fatenexx.backend.product.entity.PtProduct;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.product.entity.PtTypeConverter;
import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;

import java.time.LocalDate;

@Entity
@Table(name = "pt_passes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pt_pass_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                  // 구매한 회원

    @Convert(converter = PtTypeConverter.class)
    @Column(name = "pt_type", nullable = false, length = 10)
    private PtType ptType;              // 개인 / 그룹

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private User trainer;               // 개인 PT만 담당 트레이너, 그룹은 null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private PtProduct product;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount;     // 차감될 때마다 줄어듦

    @Column(name = "price", nullable = false)
    private Integer price;

    public static PtPass create(User user, PtProduct product, User trainer, Payment payment, LocalDate startDate) {
        PtPass p = new PtPass();
        p.user = user;
        p.ptType = product.getPtType();
        p.trainer = trainer;
        p.product = product;
        p.payment = payment;
        p.startDate = startDate;
        p.endDate = startDate.plusWeeks(product.getValidWeeks()).minusDays(1);
        p.totalCount = product.getTotalCount();
        p.remainingCount = product.getTotalCount();
        p.price = product.getPrice();
        return p;
    }

    // 1회 차감 (A 추가: 회원 차감 취소, 출석·결석 처리에서 사용)
    public void deduct() {
        if (remainingCount <= 0) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_COUNT);
        }
        this.remainingCount--;
    }
}
