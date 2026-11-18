package com.fatenexx.backend.user.repository;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByPhone(String phone);

    // 키오스크 체크인: 전화번호로 회원 찾기
    Optional<User> findByPhone(String phone);

    // 본인을 제외하고 같은 전화번호가 있는지 (내 정보 수정 시 중복 검사)
    boolean existsByPhoneAndIdNot(String phone, Long id);

    // 역할까지 맞는 사용자만 조회 (회원 상세에서 트레이너·관리자 ID를 걸러냄)
    Optional<User> findByIdAndRole(Long id, Role role);

    // 역할별 목록 + 이름·전화번호·아이디 검색 (keyword가 null이면 전체)
    @Query("select u from User u where u.role = :role and " +
            "(:keyword is null or u.name like concat('%', :keyword, '%') " +
            "or u.phone like concat('%', :keyword, '%') " +
            "or u.loginId like concat('%', :keyword, '%'))")
    Page<User> searchByRole(@Param("role") Role role,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    // 결제·체크인 시 같은 회원의 동시 요청을 막기 위해 회원 행을 잠그고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}