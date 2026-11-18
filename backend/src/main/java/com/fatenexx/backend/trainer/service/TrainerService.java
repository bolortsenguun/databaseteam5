package com.fatenexx.backend.trainer.service;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.trainer.dto.TrainerListResponse;
import com.fatenexx.backend.trainer.dto.TrainerSummaryResponse;
import com.fatenexx.backend.trainer.dto.WorkTimeResponse;
import com.fatenexx.backend.trainer.repository.TrainerScheduleRepository;
import com.fatenexx.backend.user.entity.User;
import com.fatenexx.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.trainer.dto.AdminTrainerListResponse;
import com.fatenexx.backend.trainer.dto.AdminTrainerResponse;
import com.fatenexx.backend.user.entity.ApprovalStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fatenexx.backend.trainer.dto.TrainerApproveResponse;
import com.fatenexx.backend.trainer.dto.TrainerRejectResponse;
import com.fatenexx.backend.trainer.dto.AdminTrainerDetailResponse;
import com.fatenexx.backend.user.entity.Gender;

import java.util.Set;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerService {

    private final UserRepository userRepository;
    private final TrainerScheduleRepository scheduleRepository;
    @PersistenceContext
    private EntityManager em;


    // ① 회원용 트레이너 목록 (승인된 트레이너만)
    public TrainerListResponse getTrainers() {
        List<User> trainers = userRepository
                .searchByRole(Role.TRAINER, null, Pageable.unpaged())
                .stream()
                .filter(User::isApproved)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        Map<Long, List<WorkTimeResponse>> workTimes = getWorkTimes(
                trainers.stream().map(User::getId).toList());

        List<TrainerSummaryResponse> items = trainers.stream()
                .map(t -> TrainerSummaryResponse.of(t, workTimes.getOrDefault(t.getId(), List.of())))
                .toList();
        return new TrainerListResponse(items);
    }

    // ② 관리자 트레이너 목록
    public AdminTrainerListResponse getTrainersForAdmin(String approvalStatus, int page, int size) {
        if (page < 1 || size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
        }
        ApprovalStatus filter = parseApprovalStatus(approvalStatus);

        // 승인대기 먼저(오래된 순), 그다음 승인(최신 순)
        Comparator<User> order = Comparator
                .comparing((User u) -> u.isApproved())
                .thenComparing((a, b) -> a.isApproved()
                        ? b.getCreatedAt().compareTo(a.getCreatedAt())
                        : a.getCreatedAt().compareTo(b.getCreatedAt()));

        List<User> all = userRepository.searchByRole(Role.TRAINER, null, Pageable.unpaged()).stream()
                .filter(u -> filter == null || u.getApprovalStatus() == filter)
                .sorted(order)
                .toList();

        int totalCount = all.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        List<User> pageItems = all.stream().skip((long) (page - 1) * size).limit(size).toList();

        List<Long> ids = pageItems.stream().map(User::getId).toList();
        Map<Long, List<WorkTimeResponse>> workTimes = getWorkTimes(ids);
        Map<Long, Long> clientCounts = getClientCounts(ids);

        List<AdminTrainerResponse> items = pageItems.stream()
                .map(t -> AdminTrainerResponse.of(t,
                        clientCounts.getOrDefault(t.getId(), 0L),
                        workTimes.getOrDefault(t.getId(), List.of())))
                .toList();
        return new AdminTrainerListResponse(items, page, size, totalCount, totalPages);
    }

    // 관리자 트레이너 단건 조회 (승인대기 포함)
    public AdminTrainerDetailResponse getTrainerForAdmin(Long trainerId) {
        return AdminTrainerDetailResponse.from(findTrainer(trainerId));
    }

    // ③ 트레이너 가입 승인
    @Transactional
    public TrainerApproveResponse approve(Long trainerId) {
        User trainer = findTrainer(trainerId);
        if (trainer.isApproved()) {
            throw new BusinessException(ErrorCode.ALREADY_APPROVED);
        }
        trainer.approve();
        return new TrainerApproveResponse(trainerId, trainer.getApprovalStatus().getLabel());
    }

    // ④ 트레이너 가입 거절 → 계정 삭제 (BR-ACC-04)
    @Transactional
    public TrainerRejectResponse reject(Long trainerId) {
        User trainer = findTrainer(trainerId);
        if (trainer.isApproved()) {
            throw new BusinessException(ErrorCode.ALREADY_APPROVED, "승인된 트레이너의 가입을 거절할 수 없습니다.");
        }
        userRepository.delete(trainer);
        return new TrainerRejectResponse(trainerId, true);
    }

    private User findTrainer(Long trainerId) {
        return userRepository.findByIdAndRole(trainerId, Role.TRAINER)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));
    }

    // ⑤ 관리자 트레이너 정보 수정 (생략 = 유지, null = 삭제)
    @Transactional
    public AdminTrainerDetailResponse updateTrainer(Long trainerId, Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수정할 항목을 하나 이상 입력해 주세요.");
        }
        for (String key : body.keySet()) {
            if (!EDITABLE_FIELDS.contains(key)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, key + "은(는) 수정할 수 없습니다.");
            }
        }
        User t = findTrainer(trainerId);

        String name = body.containsKey("name") ? requiredText(body.get("name"), 50, "이름") : t.getName();
        String phone = body.containsKey("phone") ? parsePhone(body.get("phone")) : t.getPhone();
        LocalDate birthDate = body.containsKey("birthDate") ? parseBirthDate(body.get("birthDate")) : t.getBirthDate();
        Gender gender = body.containsKey("gender") ? parseGender(body.get("gender")) : t.getGender();
        String career = body.containsKey("career") ? optionalText(body.get("career"), 200, "경력") : t.getCareer();
        String introduction = body.containsKey("introduction")
                ? optionalText(body.get("introduction"), 1000, "소개") : t.getIntroduction();

        if (!phone.equals(t.getPhone()) && userRepository.existsByPhoneAndIdNot(phone, trainerId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        t.updateTrainerInfo(name, phone, birthDate, gender, career, introduction);
        return AdminTrainerDetailResponse.from(t);
    }

    private static final Set<String> EDITABLE_FIELDS =
            Set.of("name", "phone", "birthDate", "gender", "career", "introduction");

    // 필수 글자 (null·빈 값 불가)
    private String requiredText(Object value, int max, String label) {
        if (!(value instanceof String s) || s.isBlank() || s.length() > max) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, label + "은(는) 1~" + max + "자로 입력해 주세요.");
        }
        return s;
    }

    // 선택 글자 (null·빈 값이면 삭제)
    private String optionalText(Object value, int max, String label) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String s) || s.length() > max) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, label + "은(는) 최대 " + max + "자입니다.");
        }
        return s.isBlank() ? null : s;
    }

    // 010 + 숫자 8자리
    private String parsePhone(Object value) {
        if (!(value instanceof String s) || !s.matches("^010\\d{8}$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "전화번호는 010으로 시작하는 숫자 11자리입니다.");
        }
        return s;
    }

    // YYYY-MM-DD, 미래 날짜 불가, null이면 삭제
    private LocalDate parseBirthDate(Object value) {
        if (value == null) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse((String) value);
            if (date.isAfter(LocalDate.now())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "생년월일은 미래 날짜일 수 없습니다.");
            }
            return date;
        } catch (ClassCastException | java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "생년월일은 YYYY-MM-DD 형식입니다.");
        }
    }

    // "남" / "여", null이면 삭제
    private Gender parseGender(Object value) {
        if (value == null) {
            return null;
        }
        for (Gender g : Gender.values()) {
            if (g.getLabel().equals(value)) {
                return g;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "성별은 남 또는 여입니다.");
    }

    // "전체"·null → 필터 없음, "승인대기" / "승인" → 해당 상태, 그 외 → 400
    private ApprovalStatus parseApprovalStatus(String value) {
        if (value == null || value.equals("전체")) {
            return null;
        }
        for (ApprovalStatus s : ApprovalStatus.values()) {
            if (s.getLabel().equals(value)) {
                return s;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 조회 조건을 입력해 주세요.");
    }

    // 트레이너별 담당 회원 수 (유효기간 안 끝난 개인 PT 이용권 기준, 같은 회원은 1명)
    private Map<Long, Long> getClientCounts(List<Long> trainerIds) {
        if (trainerIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = em.createQuery(
                        "select p.trainer.id, count(distinct p.user.id) from PtPass p " +
                                "where p.trainer.id in :ids and p.endDate >= :today " +
                                "group by p.trainer.id", Object[].class)
                .setParameter("ids", trainerIds)
                .setParameter("today", LocalDate.now())
                .getResultList();
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    // 트레이너별 근무 시간을 한 번에 꺼내서 트레이너 ID로 묶기
    private Map<Long, List<WorkTimeResponse>> getWorkTimes(List<Long> trainerIds) {
        if (trainerIds.isEmpty()) {
            return Map.of();
        }
        return scheduleRepository.findByTrainerIdInOrderByDayOfWeekAscStartTimeAsc(trainerIds).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTrainer().getId(),
                        Collectors.mapping(WorkTimeResponse::from, Collectors.toList())));
    }
}