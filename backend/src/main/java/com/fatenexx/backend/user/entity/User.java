package com.fatenexx.backend.user.entity;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.RoleConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "login_id", nullable = false, length = 20, updatable = false)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Convert(converter = ApprovalStatusConverter.class)
    @Column(name = "approval_status", nullable = false, length = 10)
    private ApprovalStatus approvalStatus;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "career", length = 200)
    private String career;

    @Column(name = "introduction", length = 1000)
    private String introduction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 회원가입용 생성 메서드: 승인 상태·비번 변경 여부는 여기서 규칙대로 정함
    public static User register(String loginId, String passwordHash, Role role,
                                String name, String phone, LocalDate birthDate, Gender gender) {
        User user = new User();
        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.role = role;
        user.name = name;
        user.phone = phone;
        user.birthDate = birthDate;
        user.gender = gender;
        user.mustChangePassword = false;
        user.approvalStatus = (role == Role.TRAINER) ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED;  // BR-ACC-04
        return user;
    }

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    public void resetPassword(String temporaryPasswordHash) {
        this.passwordHash = temporaryPasswordHash;
        this.mustChangePassword = true;   // 초기화 후 다음 로그인 때 변경 강제 (BR-ACC-07)
    }

    // 값이 null이면 바꾸지 않음
    public void updateProfile(String name, String phone, LocalDate birthDate, Gender gender) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
    }

    // 트레이너 경력·소개글. 빈 문자열("")을 보내면 삭제(NULL)
    public void updateTrainerProfile(String career, String introduction) {
        if (career != null) this.career = career.isBlank() ? null : career;
        if (introduction != null) this.introduction = introduction.isBlank() ? null : introduction;
    }

    // 트레이너 가입 승인 (A: 관리자 트레이너 승인 API에서 사용)
    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }
}