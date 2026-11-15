package com.fatenexx.backend.setting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Setting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String settingKey;          // 예: CANCEL_DEADLINE_HOURS

    @Column(name = "setting_value", nullable = false)
    private Integer settingValue;       // 예: 24

    @Column(name = "description", length = 200)
    private String description;

    public void changeValue(int value) {
        this.settingValue = value;
    }
}