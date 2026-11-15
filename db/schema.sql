-- =====================================================================
-- 헬스장 회원·PT 예약 관리 시스템 DB 스키마 (MySQL 8.0.16+)
-- 업무 규칙 개정본(개인·그룹 PT, 트레이너 가입 승인, 결과 대기 흐름) 반영
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 사용자 (회원/트레이너/관리자 공통)
-- ---------------------------------------------------------------------
CREATE TABLE users (
                       user_id               BIGINT       NOT NULL AUTO_INCREMENT,
                       login_id              VARCHAR(20)  NOT NULL COMMENT '본인이 직접 정함, 이후 변경 불가 (BR-ACC-02)',
                       password_hash         VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
                       must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '관리자 seed·관리자 초기화 시 TRUE (BR-ACC-06, 07)',
                       role                  VARCHAR(10)  NOT NULL COMMENT '회원/트레이너/관리자',
                       approval_status       VARCHAR(10)  NOT NULL DEFAULT '승인' COMMENT '트레이너 가입 시 서버가 승인대기로 저장 (BR-ACC-04)',
                       name                  VARCHAR(50)  NOT NULL,
                       phone                 VARCHAR(20)  NOT NULL,
                       birth_date            DATE         NULL,
                       gender                VARCHAR(10)  NULL,
                       career                VARCHAR(200) NULL COMMENT '트레이너 경력, 선택 입력. NULL이면 "등록된 정보가 없어요" 표시',
                       introduction          VARCHAR(1000) NULL COMMENT '트레이너 소개글, 선택 입력. NULL이면 "등록된 정보가 없어요" 표시',
                       created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (user_id),
                       UNIQUE KEY uq_users_login_id (login_id),
                       UNIQUE KEY uq_users_phone (phone),
                       CONSTRAINT ck_users_role CHECK (role IN ('회원', '트레이너', '관리자')),
                       CONSTRAINT ck_users_approval CHECK (approval_status IN ('승인대기', '승인')),
                       CONSTRAINT ck_users_approval_role CHECK (role = '트레이너' OR approval_status = '승인')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. 설정 (정책 숫자 모음)
-- ---------------------------------------------------------------------
CREATE TABLE settings (
                          setting_key    VARCHAR(50)  NOT NULL COMMENT '예: CANCEL_DEADLINE_HOURS',
                          setting_value  INT          NOT NULL,
                          description    VARCHAR(200) NULL,
                          PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. 상품
-- ---------------------------------------------------------------------
CREATE TABLE membership_products (
                                     product_id  BIGINT      NOT NULL AUTO_INCREMENT,
                                     name        VARCHAR(50) NOT NULL COMMENT '예: 1개월권',
                                     months      INT         NOT NULL COMMENT '1, 3, 6, 12 (BR-PASS-06)',
                                     price       INT         NOT NULL COMMENT '원 단위',
                                     is_active   BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '삭제 불가, 판매 중지만 (BR-PASS-12)',
                                     PRIMARY KEY (product_id),
                                     CONSTRAINT ck_mproducts_price CHECK (price >= 0),
                                     CONSTRAINT ck_mproducts_months CHECK (months > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pt_products (
                             product_id   BIGINT      NOT NULL AUTO_INCREMENT,
                             pt_type      VARCHAR(10) NOT NULL COMMENT '개인/그룹 (BR-TRN-04)',
                             name         VARCHAR(50) NOT NULL COMMENT '예: 개인 PT 10회',
                             total_count  INT         NOT NULL COMMENT '10, 20, 30',
                             valid_weeks  INT         NOT NULL COMMENT '12, 24, 36',
                             price        INT         NOT NULL COMMENT '원 단위',
                             is_active    BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '삭제 불가, 판매 중지만 (BR-PASS-12)',
                             PRIMARY KEY (product_id),
                             CONSTRAINT ck_ptproducts_type CHECK (pt_type IN ('개인', '그룹')),
                             CONSTRAINT ck_ptproducts_price CHECK (price >= 0),
                             CONSTRAINT ck_ptproducts_count CHECK (total_count > 0 AND valid_weeks > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. 결제 (온라인 모의결제만, 현장 결제 없음 BR-PASS-01)
-- ---------------------------------------------------------------------
CREATE TABLE payments (
                          payment_id  BIGINT      NOT NULL AUTO_INCREMENT,
                          user_id     BIGINT      NOT NULL COMMENT '결제한 회원',
                          amount      INT         NOT NULL COMMENT '상품 가격 그대로 (BR-PASS-04, 05)',
                          status      VARCHAR(10) NOT NULL COMMENT '성공/실패 (BR-PASS-03)',
                          paid_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매출 통계 기준 (BR-STAT-01)',
                          PRIMARY KEY (payment_id),
                          CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (user_id),
                          CONSTRAINT ck_payments_status CHECK (status IN ('성공', '실패')),
                          CONSTRAINT ck_payments_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 5. 이용권
-- ---------------------------------------------------------------------
CREATE TABLE memberships (
                             membership_id  BIGINT NOT NULL AUTO_INCREMENT,
                             user_id        BIGINT NOT NULL,
                             product_id     BIGINT NOT NULL,
                             payment_id     BIGINT NOT NULL COMMENT '결제 1건당 회원권 1개',
                             start_date     DATE   NOT NULL,
                             end_date       DATE   NOT NULL COMMENT '시작일부터 계산하여 저장',
                             price          INT    NOT NULL COMMENT '구매 당시 가격 유지 (BR-PASS-08)',
                             PRIMARY KEY (membership_id),
                             UNIQUE KEY uq_memberships_payment (payment_id),
                             CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (user_id),
                             CONSTRAINT fk_memberships_product FOREIGN KEY (product_id) REFERENCES membership_products (product_id),
                             CONSTRAINT fk_memberships_payment FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
                             CONSTRAINT ck_memberships_period CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pt_passes (
                           pt_pass_id       BIGINT      NOT NULL AUTO_INCREMENT,
                           user_id          BIGINT      NOT NULL COMMENT '구매한 회원',
                           pt_type          VARCHAR(10) NOT NULL COMMENT '개인/그룹. 상품 종류를 복사 (BR-GRP-06)',
                           trainer_id       BIGINT      NULL COMMENT '개인 PT만 담당 트레이너, 그룹은 NULL (BR-PASS-09, GRP-05)',
                           product_id       BIGINT      NOT NULL,
                           payment_id       BIGINT      NOT NULL COMMENT '결제 1건당 PT 이용권 1개',
                           start_date       DATE        NOT NULL,
                           end_date         DATE        NOT NULL COMMENT '시작일 + 유효 주 수',
                           total_count      INT         NOT NULL COMMENT '구매 당시 값 복사',
                           remaining_count  INT         NOT NULL COMMENT '차감될 때마다 감소 (BR-CNL)',
                           price            INT         NOT NULL COMMENT '구매 당시 가격 유지 (BR-PASS-08)',
                           PRIMARY KEY (pt_pass_id),
                           UNIQUE KEY uq_ptpasses_payment (payment_id),
                           CONSTRAINT fk_ptpasses_user FOREIGN KEY (user_id) REFERENCES users (user_id),
                           CONSTRAINT fk_ptpasses_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id),
                           CONSTRAINT fk_ptpasses_product FOREIGN KEY (product_id) REFERENCES pt_products (product_id),
                           CONSTRAINT fk_ptpasses_payment FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
                           CONSTRAINT ck_ptpasses_type CHECK (pt_type IN ('개인', '그룹')),
                           CONSTRAINT ck_ptpasses_trainer CHECK (
                               (pt_type = '개인' AND trainer_id IS NOT NULL) OR (pt_type = '그룹' AND trainer_id IS NULL)
                               ),
                           CONSTRAINT ck_ptpasses_period CHECK (end_date >= start_date),
                           CONSTRAINT ck_ptpasses_count CHECK (remaining_count >= 0 AND remaining_count <= total_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 6. 트레이너 근무
-- ---------------------------------------------------------------------
CREATE TABLE trainer_schedules (
                                   schedule_id  BIGINT NOT NULL AUTO_INCREMENT,
                                   trainer_id   BIGINT NOT NULL,
                                   day_of_week  INT    NOT NULL COMMENT '1=월 ~ 7=일',
                                   start_time   TIME   NOT NULL,
                                   end_time     TIME   NOT NULL,
                                   PRIMARY KEY (schedule_id),
                                   CONSTRAINT fk_schedules_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id),
                                   CONSTRAINT ck_schedules_dow CHECK (day_of_week BETWEEN 1 AND 7),
                                   CONSTRAINT ck_schedules_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trainer_days_off (
                                  day_off_id  BIGINT NOT NULL AUTO_INCREMENT,
                                  trainer_id  BIGINT NOT NULL,
                                  off_date    DATE   NOT NULL,
                                  PRIMARY KEY (day_off_id),
                                  UNIQUE KEY uq_daysoff_trainer_date (trainer_id, off_date),
                                  CONSTRAINT fk_daysoff_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 7. 그룹 PT 수업 (BR-GRP)
-- slot_active: 폐강·취소가 아니면 1, 맞으면 NULL
--   → 같은 트레이너가 같은 시각에 살아있는 그룹 수업을 두 개 열 수 없게 함
--   → 개인 PT 예약과의 겹침(BR-GRP-04)은 서버에서 검사
-- ---------------------------------------------------------------------
CREATE TABLE group_classes (
                               class_id       BIGINT       NOT NULL AUTO_INCREMENT,
                               trainer_id     BIGINT       NOT NULL COMMENT '개설한 트레이너 (승인된 트레이너)',
                               name           VARCHAR(100) NOT NULL COMMENT '수업명',
                               description    VARCHAR(200) NULL COMMENT '그룹 PT 수업 설명, 선택 입력 (최대 200자)',
                               class_date     DATE         NOT NULL,
                               start_time     TIME         NOT NULL COMMENT '정시만 (BR-GRP-02)',
                               end_time       TIME         NOT NULL COMMENT '시작 + 1시간 고정',
                               capacity       TINYINT      NOT NULL COMMENT '정원 4/5/6 (BR-GRP-03)',
                               status         VARCHAR(10)  NOT NULL DEFAULT '개설' COMMENT '개설/확정/폐강/취소 (BR-GRP-12, 13)',
                               cancel_reason  VARCHAR(200) NULL COMMENT '트레이너 사정 취소 사유',
                               canceled_at    DATETIME     NULL,
                               created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               slot_active    TINYINT GENERATED ALWAYS AS (CASE WHEN status IN ('폐강', '취소') THEN NULL ELSE 1 END) STORED,
                               PRIMARY KEY (class_id),
                               UNIQUE KEY uq_gclass_trainer_slot (trainer_id, class_date, start_time, slot_active),
                               KEY idx_gclass_date (class_date),
                               CONSTRAINT fk_gclass_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id),
                               CONSTRAINT ck_gclass_capacity CHECK (capacity IN (4, 5, 6)),
                               CONSTRAINT ck_gclass_status CHECK (status IN ('개설', '확정', '폐강', '취소')),
                               CONSTRAINT ck_gclass_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 8. PT 예약 (개인 PT 1건 = 1행, 그룹 PT는 참여자마다 1행)
-- group_class_id: NULL이면 개인 PT, 값이 있으면 그룹 PT 신청
-- lesson_date/start_time/end_time/trainer_id는 그룹도 수업에서 복사해 저장
--   (참여자가 있는 그룹 수업은 수정 불가라 어긋나지 않음, BR-GRP-08)
-- active_flag       : 취소가 아니면 1 → 회원 하루 1회(개인·그룹 합산, BR-RSV-05)
-- individual_flag   : 취소가 아닌 개인 PT면 1 → 슬롯당 1명 (BR-RSV-02)
-- ---------------------------------------------------------------------
CREATE TABLE pt_reservations (
                                 reservation_id  BIGINT       NOT NULL AUTO_INCREMENT,
                                 pt_pass_id      BIGINT       NOT NULL COMMENT '예약 시 자동 선택된 이용권 (BR-CNL-08)',
                                 user_id         BIGINT       NOT NULL COMMENT '예약한 회원',
                                 trainer_id      BIGINT       NOT NULL,
                                 group_class_id  BIGINT       NULL COMMENT '그룹 PT일 때만',
                                 lesson_date     DATE         NOT NULL,
                                 start_time      TIME         NOT NULL,
                                 end_time        TIME         NOT NULL,
                                 status          VARCHAR(10)  NOT NULL DEFAULT '예약' COMMENT '예약/결과대기/출석/결석/취소 (BR-CNL-02)',
                                 canceled_by     VARCHAR(10)  NULL COMMENT '회원/트레이너/관리자/폐강, 취소일 때만',
                                 cancel_reason   VARCHAR(200) NULL COMMENT '트레이너·관리자 취소 시 필수 (BR-CNL-06)',
                                 canceled_at     DATETIME     NULL,
                                 is_deducted     BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '차감 여부. 차감 후 취소(24시간 이내)면 재신청 불가 (BR-RSV-10)',
                                 reserved_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '10분 무료 취소 판정 기준 (BR-CNL-11)',
                                 active_flag     TINYINT GENERATED ALWAYS AS (CASE WHEN status = '취소' THEN NULL ELSE 1 END) STORED,
                                 individual_flag TINYINT GENERATED ALWAYS AS (CASE WHEN status <> '취소' AND group_class_id IS NULL THEN 1 ELSE NULL END) STORED,
                                 PRIMARY KEY (reservation_id),
                                 UNIQUE KEY uq_resv_individual_slot (trainer_id, lesson_date, start_time, individual_flag),
                                 UNIQUE KEY uq_resv_user_day (user_id, lesson_date, active_flag),
                                 KEY idx_resv_group (group_class_id),
                                 KEY idx_resv_status_date (status, lesson_date),
                                 CONSTRAINT fk_resv_pass FOREIGN KEY (pt_pass_id) REFERENCES pt_passes (pt_pass_id),
                                 CONSTRAINT fk_resv_user FOREIGN KEY (user_id) REFERENCES users (user_id),
                                 CONSTRAINT fk_resv_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id),
                                 CONSTRAINT fk_resv_group FOREIGN KEY (group_class_id) REFERENCES group_classes (class_id),
                                 CONSTRAINT ck_resv_status CHECK (status IN ('예약', '결과대기', '출석', '결석', '취소')),
                                 CONSTRAINT ck_resv_canceled_by CHECK (canceled_by IS NULL OR canceled_by IN ('회원', '트레이너', '관리자', '폐강')),
                                 CONSTRAINT ck_resv_cancel_state CHECK (
                                     (status = '취소' AND canceled_by IS NOT NULL) OR (status <> '취소' AND canceled_by IS NULL)
                                     ),
                                 CONSTRAINT ck_resv_reason_required CHECK (canceled_by IS NULL OR canceled_by NOT IN ('트레이너', '관리자') OR cancel_reason IS NOT NULL),
                                 CONSTRAINT ck_resv_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 9. 출석 (키오스크 체크인)
-- ---------------------------------------------------------------------
CREATE TABLE attendances (
                             attendance_id   BIGINT      NOT NULL AUTO_INCREMENT,
                             user_id         BIGINT      NOT NULL,
                             checked_in_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             entry_type      VARCHAR(10) NOT NULL COMMENT '회원권/PT (BR-ATT-01)',
                             reservation_id  BIGINT      NULL COMMENT 'PT로 입장했을 때만 (개인·그룹 공통)',
                             PRIMARY KEY (attendance_id),
                             KEY idx_attendances_user_time (user_id, checked_in_at),
                             KEY idx_attendances_time (checked_in_at),
                             CONSTRAINT fk_attendances_user FOREIGN KEY (user_id) REFERENCES users (user_id),
                             CONSTRAINT fk_attendances_resv FOREIGN KEY (reservation_id) REFERENCES pt_reservations (reservation_id),
                             CONSTRAINT ck_attendances_type CHECK (entry_type IN ('회원권', 'PT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 초기 데이터
-- =====================================================================

-- 정책 숫자 (문서 개요)
INSERT INTO settings (setting_key, setting_value, description) VALUES
                                                                   ('CANCEL_DEADLINE_HOURS',    24, '취소 기한(시간). 이내 회원 취소 시 1회 차감'),
                                                                   ('FREE_CANCEL_MINUTES',      10, '예약 후 무료 취소 가능 시간(분)'),
                                                                   ('BOOKING_RANGE_DAYS',       14, '예약 가능 범위(일), 회원 목록 표시 범위'),
                                                                   ('BOOKING_CLOSE_HOURS',       1, '예약 마감(수업 시작 N시간 전)'),
                                                                   ('SLOT_MINUTES',             60, '슬롯 단위(분)'),
                                                                   ('GROUP_OPEN_MIN_HOURS',     48, '그룹 수업 개설 마감(시작 N시간 전까지)'),
                                                                   ('GROUP_OPEN_MAX_DAYS',      30, '그룹 수업 개설 가능 범위(오늘부터 N일 후)'),
                                                                   ('GROUP_MIN_PARTICIPANTS',    2, '그룹 수업 최소 인원'),
                                                                   ('GROUP_CONFIRM_HOURS',      24, '그룹 최소 인원 판정 시점(시작 N시간 전)'),
                                                                   ('PT_ENTRY_BEFORE_MINUTES',  30, 'PT 회원 입장 가능(시작 N분 전부터)'),
                                                                   ('PT_ENTRY_AFTER_MINUTES',   30, 'PT 회원 입장 가능(시작 N분 후까지)'),
                                                                   ('PASS_START_MAX_DAYS',      30, '이용권 시작일 선택 가능 범위(일)'),
                                                                   ('RECHECKIN_MINUTES',        10, '재체크인 중복 판정(분)');

-- 회원권 기본 상품 (BR-PASS-06). 가격은 예시값이므로 실제 가격으로 교체할 것
INSERT INTO membership_products (name, months, price) VALUES
                                                          ('1개월권',   1,  50000),
                                                          ('3개월권',   3, 130000),
                                                          ('6개월권',   6, 240000),
                                                          ('12개월권', 12, 440000);

-- 개인·그룹 PT 기본 상품 (BR-TRN-04). 가격은 예시값이므로 실제 가격으로 교체할 것
INSERT INTO pt_products (pt_type, name, total_count, valid_weeks, price) VALUES
                                                                             ('개인', '개인 PT 10회', 10, 12,  500000),
                                                                             ('개인', '개인 PT 20회', 20, 24,  950000),
                                                                             ('개인', '개인 PT 30회', 30, 36, 1350000),
                                                                             ('그룹', '그룹 PT 10회', 10, 12,  300000),
                                                                             ('그룹', '그룹 PT 20회', 20, 24,  570000),
                                                                             ('그룹', '그룹 PT 30회', 30, 36,  810000);

-- 관리자 계정은 BR-ACC-06에 따라 seed로 등록하고 첫 로그인 시 비밀번호 변경을 강제한다.
-- password_hash는 백엔드의 해시 방식(BCrypt 등)으로 만든 값을 넣어야 한다.
-- INSERT INTO users (login_id, password_hash, must_change_password, role, approval_status, name, phone)
-- VALUES ('admin', '<해시값>', TRUE, '관리자', '승인', '관리자', '01000000000');
