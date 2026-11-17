package com.fatenexx.backend.setting.repository;

import com.fatenexx.backend.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
}