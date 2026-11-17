package com.fatenexx.backend.setting.service;

import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingService {

    private final SettingRepository settingRepository;

    // 정책 숫자는 항상 이 메서드로 조회 (코드에 숫자를 직접 쓰지 않기)
    public int getInt(String key) {
        return settingRepository.findById(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND,
                        "설정값을 찾을 수 없습니다: " + key))
                .getSettingValue();
    }
}