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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettingAdminService {

    // 0이 되면 예약 시간 계산 등이 깨지는 설정은 1 이상만 허용
    private static final Set<String> MIN_ONE = Set.of(
            SettingKey.SLOT_MINUTES,
            SettingKey.GROUP_MIN_PARTICIPANTS,
            SettingKey.BOOKING_RANGE_DAYS);

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

        for (SettingChangeRequest c : request.settings()) {
            Setting setting = settingRepository.findById(c.key())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT,
                            "존재하지 않는 설정입니다: " + c.key()));
            if (MIN_ONE.contains(c.key()) && c.value() < 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, c.key() + " 은(는) 1 이상이어야 합니다.");
            }
            setting.changeValue(c.value());
        }

        return toResponse();
    }

    private SettingListResponse toResponse() {
        List<SettingItemResponse> items = settingRepository.findAll(Sort.by("settingKey")).stream()
                .map(s -> new SettingItemResponse(s.getSettingKey(), s.getSettingValue(), s.getDescription()))
                .toList();
        return new SettingListResponse(items);
    }
}