package com.fatenexx.backend.payment.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.pass.entity.Membership;
import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.pass.repository.MembershipRepository;
import com.fatenexx.backend.pass.repository.PtPassRepository;
import com.fatenexx.backend.payment.dto.*;
import com.fatenexx.backend.payment.entity.Payment;
import com.fatenexx.backend.payment.entity.PaymentStatus;
import com.fatenexx.backend.payment.repository.PaymentRepository;
import com.fatenexx.backend.product.entity.MembershipProduct;
import com.fatenexx.backend.product.entity.PtProduct;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.product.repository.MembershipProductRepository;
import com.fatenexx.backend.product.repository.PtProductRepository;
import com.fatenexx.backend.setting.SettingKey;
import com.fatenexx.backend.setting.service.SettingService;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
// ↓ BusinessException, ErrorCode 는 ProductService 의 import 와 똑같이 맞추세요
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PaymentRepository paymentRepository;
    private final MembershipRepository membershipRepository;
    private final PtPassRepository ptPassRepository;
    private final MembershipProductRepository membershipProductRepository;
    private final PtProductRepository ptProductRepository;
    private final UserRepository userRepository;
    private final SettingService settingService;

    private record PassInfo(PurchaseType type, String productName, LocalDate start, LocalDate end) {
    }

    /** 모의 결제 + 이용권 생성 (회원 본인) */
    @Transactional
    public PaymentResultResponse simulate(Long userId, SimulatePaymentRequest req) {
        // 같은 회원의 동시 구매로 기간이 겹치지 않도록 회원 행을 잠근다
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean fail = Boolean.TRUE.equals(req.simulateFail());

        if (req.purchaseType() == PurchaseType.MEMBERSHIP) {
            return buyMembership(user, req, fail);
        }
        return buyPt(user, req, fail);
    }

    private PaymentResultResponse buyMembership(User user, SimulatePaymentRequest req, boolean fail) {
        MembershipProduct product = membershipProductRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        LocalDate today = LocalDate.now(KST);
        LocalDate latestEnd = membershipRepository.findLatestEndDate(user.getId(), today);
        LocalDate defaultStart = (latestEnd == null) ? today : latestEnd.plusDays(1);
        LocalDate start = resolveStartDate(req.startDate(), defaultStart);

        Payment payment = paymentRepository.save(
                Payment.create(user, product.getPrice(), fail ? PaymentStatus.FAIL : PaymentStatus.SUCCESS));
        if (fail) {
            return PaymentResultResponse.of(payment, PurchaseType.MEMBERSHIP, product.getName(), null, null);
        }
        Membership m = membershipRepository.save(Membership.create(user, product, payment, start));
        return PaymentResultResponse.of(payment, PurchaseType.MEMBERSHIP, product.getName(),
                m.getStartDate(), m.getEndDate());
    }

    private PaymentResultResponse buyPt(User user, SimulatePaymentRequest req, boolean fail) {
        PtProduct product = ptProductRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        User trainer = null;
        if (product.getPtType() == PtType.PERSONAL) {
            if (req.trainerId() == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "선택할 수 없는 트레이너입니다.");
            }
            trainer = userRepository.findByIdAndRole(req.trainerId(), Role.TRAINER)
                    .filter(User::isApproved)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "선택할 수 없는 트레이너입니다."));
        } else if (req.trainerId() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "그룹 PT는 트레이너를 선택할 수 없습니다.");
        }

        LocalDate start = resolveStartDate(req.startDate(), LocalDate.now(KST));

        Payment payment = paymentRepository.save(
                Payment.create(user, product.getPrice(), fail ? PaymentStatus.FAIL : PaymentStatus.SUCCESS));
        if (fail) {
            return PaymentResultResponse.of(payment, PurchaseType.PT, product.getName(), null, null);
        }
        PtPass pass = ptPassRepository.save(PtPass.create(user, product, trainer, payment, start));
        return PaymentResultResponse.of(payment, PurchaseType.PT, product.getName(),
                pass.getStartDate(), pass.getEndDate());
    }

    /** 시작일: 미입력이면 기본값, 입력하면 기본값 ~ 기본값+PASS_START_MAX_DAYS 안이어야 함 */
    private LocalDate resolveStartDate(LocalDate requested, LocalDate defaultStart) {
        if (requested == null) {
            return defaultStart;
        }
        int maxDays = settingService.getInt(SettingKey.PASS_START_MAX_DAYS);
        if (requested.isBefore(defaultStart) || requested.isAfter(defaultStart.plusDays(maxDays))) {
            throw new BusinessException(ErrorCode.INVALID_START_DATE);
        }
        return requested;
    }

    /** 내 결제 내역 */
    @Transactional(readOnly = true)
    public PaymentPageResponse<PaymentItemResponse> getMyPayments(Long userId, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<Payment> result = paymentRepository.findByUserId(userId, pageable);
        Map<Long, PassInfo> info = loadPassInfo(result.getContent());

        List<PaymentItemResponse> items = result.getContent().stream().map(p -> {
            PassInfo i = info.get(p.getId());
            return new PaymentItemResponse(p.getId(), p.getStatus().name(), p.getAmount(), p.getPaidAt(),
                    i == null ? null : i.type(), i == null ? null : i.productName(),
                    i == null ? null : i.start(), i == null ? null : i.end());
        }).toList();

        return new PaymentPageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** 관리자: 결제 목록 (회원·결과·기간 검색, 조건이 없으면 전체) */
    @Transactional(readOnly = true)
    public PaymentPageResponse<AdminPaymentItemResponse> getAllPayments(Long userId, String status,
                                                                        LocalDate startDate, LocalDate endDate,
                                                                        int page, int size) {
        Pageable pageable = toPageable(page, size);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
        }
        Page<Payment> result = paymentRepository.searchForAdmin(
                userId,
                parseStatus(status),
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(),   // 종료일 하루 전체 포함
                pageable);
        Map<Long, PassInfo> info = loadPassInfo(result.getContent());

        List<AdminPaymentItemResponse> items = result.getContent().stream().map(p -> {
            PassInfo i = info.get(p.getId());
            return new AdminPaymentItemResponse(p.getId(), p.getUser().getId(), p.getUser().getLoginId(),
                    p.getUser().getName(), p.getStatus().name(), p.getAmount(), p.getPaidAt(),
                    i == null ? null : i.type(), i == null ? null : i.productName(),
                    i == null ? null : i.start(), i == null ? null : i.end());
        }).toList();

        return new PaymentPageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    // 성공/실패 또는 SUCCESS/FAIL. 없거나 전체면 null(조건 없음)
    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equals("전체")) return null;
        for (PaymentStatus s : PaymentStatus.values()) {
            if (s.name().equals(status) || s.getLabel().equals(status)) return s;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 성공 또는 실패여야 합니다.");
    }

    private Pageable toPageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100 이어야 합니다.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    /** 결제 → (회원권 또는 PT권) 정보. 실패한 결제는 이용권이 없어 빠진다 */
    private Map<Long, PassInfo> loadPassInfo(List<Payment> payments) {
        Map<Long, PassInfo> map = new HashMap<>();
        if (payments.isEmpty()) {
            return map;
        }
        for (Membership m : membershipRepository.findByPaymentIn(payments)) {
            map.put(m.getPayment().getId(), new PassInfo(PurchaseType.MEMBERSHIP,
                    m.getProduct().getName(), m.getStartDate(), m.getEndDate()));
        }
        for (PtPass p : ptPassRepository.findByPaymentIn(payments)) {
            map.put(p.getPayment().getId(), new PassInfo(PurchaseType.PT,
                    p.getProduct().getName(), p.getStartDate(), p.getEndDate()));
        }
        return map;
    }
}