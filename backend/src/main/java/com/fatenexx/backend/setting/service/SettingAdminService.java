package com.fatenexx.backend.setting.service;

import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.setting.SettingKey;
import com.fatenexx.backend.setting.dto.*;
import com.fatenexx.backend.setting.entity.Setting;
import com.fatenexx.backend.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettingAdminService {

    // 항목별 최솟값 (관리자 설정 화면과 같은 기준)
    private static final Map<String, Integer> MIN_VALUE = Map.ofEntries(
            Map.entry(SettingKey.CANCEL_DEADLINE_HOURS, 1),
            Map.entry(SettingKey.FREE_CANCEL_MINUTES, 0),
            Map.entry(SettingKey.BOOKING_RANGE_DAYS, 1),
            Map.entry(SettingKey.BOOKING_CLOSE_HOURS, 0),
            Map.entry(SettingKey.PT_ENTRY_BEFORE_MINUTES, 0),
            Map.entry(SettingKey.PT_ENTRY_AFTER_MINUTES, 0),
            Map.entry(SettingKey.RECHECKIN_MINUTES, 1),
            Map.entry(SettingKey.GROUP_MIN_PARTICIPANTS, 2),
            Map.entry(SettingKey.GROUP_CONFIRM_HOURS, 1),
            Map.entry(SettingKey.GROUP_OPEN_MIN_HOURS, 1),
            Map.entry(SettingKey.GROUP_OPEN_MAX_DAYS, 1),
            Map.entry(SettingKey.PASS_START_MAX_DAYS, 0));

    // 그룹 수업 정원 최솟값 (DB CHECK: capacity IN (4, 5, 6))
    private static final int MIN_GROUP_CAPACITY = 4;

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public SettingListResponse getAll() {
        return toResponse();
    }

    /** 여러 설정을 한 번에 변경 (하나라도 틀리면 전부 취소) */
    @Transactional
    public SettingListResponse update(UpdateSettingsRequest request) {
        Set<String> seen = new HashSet<>();
        for (SettingChangeRequest c : request.settings()) {
            if (!seen.add(c.key())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 설정이 중복되었습니다: " + c.key());
            }
        }

        // 1) 항목별 검사 + 바뀐 뒤의 전체 값 만들기
        Map<String, Setting> current = new HashMap<>();
        for (Setting s : settingRepository.findAll()) {
            current.put(s.getSettingKey(), s);
        }
        Map<String, Integer> next = new HashMap<>();
        current.forEach((k, s) -> next.put(k, s.getSettingValue()));

        for (SettingChangeRequest c : request.settings()) {
            Setting setting = current.get(c.key());
            if (setting == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 설정입니다: " + c.key());
            }
            if (c.key().equals(SettingKey.SLOT_MINUTES) && !c.value().equals(setting.getSettingValue())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "수업 단위(SLOT_MINUTES)는 바꿀 수 없습니다.");
            }
            Integer min = MIN_VALUE.get(c.key());
            if (min != null && c.value() < min) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, c.key() + " 은(는) " + min + " 이상이어야 합니다.");
            }
            next.put(c.key(), c.value());
        }

        // 2) 값끼리 맞는지 검사 (이번에 바꾸지 않은 기존 값까지 합쳐서)
        checkConflicts(next);

        // 3) 저장
        for (SettingChangeRequest c : request.settings()) {
            current.get(c.key()).changeValue(c.value());
        }
        return toResponse();
    }

    private void checkConflicts(Map<String, Integer> v) {
        int minParticipants = v.get(SettingKey.GROUP_MIN_PARTICIPANTS);
        int confirmHours = v.get(SettingKey.GROUP_CONFIRM_HOURS);
        int openMinHours = v.get(SettingKey.GROUP_OPEN_MIN_HOURS);
        int openMaxDays = v.get(SettingKey.GROUP_OPEN_MAX_DAYS);

        if (minParticipants > MIN_GROUP_CAPACITY) {
            throw new BusinessException(ErrorCode.SETTING_CONFLICT,
                    "그룹 최소 인원은 " + MIN_GROUP_CAPACITY + "명을 넘을 수 없습니다. (정원 최솟값이 " + MIN_GROUP_CAPACITY + "명)");
        }
        if (openMinHours <= confirmHours) {
            throw new BusinessException(ErrorCode.SETTING_CONFLICT,
                    "그룹 개설 마감은 확정·폐강 판정 시점보다 길어야 합니다.");
        }
        if (openMaxDays * 24 <= openMinHours) {
            throw new BusinessException(ErrorCode.SETTING_CONFLICT,
                    "그룹 개설 가능 범위는 개설 마감보다 길어야 합니다.");
        }
    }

    private SettingListResponse toResponse() {
        List<SettingItemResponse> items = settingRepository.findAll(Sort.by("settingKey")).stream()
                .map(s -> new SettingItemResponse(s.getSettingKey(), s.getSettingValue(), s.getDescription()))
                .toList();
        return new SettingListResponse(items);
    }
}