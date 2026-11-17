SET NAMES utf8mb4;

-- 테스트 계정 (비밀번호 전부 test1234)
INSERT INTO users (user_id, login_id, password_hash, must_change_password, role, approval_status, name, phone) VALUES
                                                                                                                   (1, 'member1',  '$2b$10$dUSKoL4z9qEqptm/AFwDM.OigLNYjBMYfrKVKChisjK/k55q7zTAa', FALSE, '회원',     '승인',     '이서준', '01012345678'),
                                                                                                                   (2, 'trainer1', '$2b$10$dUSKoL4z9qEqptm/AFwDM.OigLNYjBMYfrKVKChisjK/k55q7zTAa', FALSE, '트레이너', '승인',     '김도윤', '01011112222'),
                                                                                                                   (3, 'admin',    '$2b$10$dUSKoL4z9qEqptm/AFwDM.OigLNYjBMYfrKVKChisjK/k55q7zTAa', FALSE, '관리자',   '승인',     '관리자', '01000000000'),
                                                                                                                   (4, 'trainer2', '$2b$10$dUSKoL4z9qEqptm/AFwDM.OigLNYjBMYfrKVKChisjK/k55q7zTAa', FALSE, '트레이너', '승인대기', '박지민', '01033334444'),
                                                                                                                   (5, 'member2',  '$2b$10$dUSKoL4z9qEqptm/AFwDM.OigLNYjBMYfrKVKChisjK/k55q7zTAa', FALSE, '회원',     '승인',     '최하은', '01055556666');

-- 결제 기록 (이용권은 결제가 있어야 만들 수 있음)
INSERT INTO payments (payment_id, user_id, amount, status) VALUES
                                                               (1, 1, 500000, '성공'),
                                                               (2, 1, 300000, '성공'),
                                                               (3, 5, 300000, '성공');

-- PT 이용권 (오늘 시작)
INSERT INTO pt_passes (pt_pass_id, user_id, pt_type, trainer_id, product_id, payment_id, start_date, end_date, total_count, remaining_count, price) VALUES
                                                                                                                                                        (1, 1, '개인', 2,    1, 1, CURDATE(), DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 12 WEEK), INTERVAL 1 DAY), 10, 10, 500000),
                                                                                                                                                        (2, 1, '그룹', NULL, 4, 2, CURDATE(), DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 12 WEEK), INTERVAL 1 DAY), 10, 10, 300000),
                                                                                                                                                        (3, 5, '그룹', NULL, 4, 3, CURDATE(), DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 12 WEEK), INTERVAL 1 DAY), 10, 10, 300000);

-- 트레이너1 근무: 월~금 10:00~18:00
INSERT INTO trainer_schedules (trainer_id, day_of_week, start_time, end_time) VALUES
                                                                                  (2, 1, '10:00', '18:00'),
                                                                                  (2, 2, '10:00', '18:00'),
                                                                                  (2, 3, '10:00', '18:00'),
                                                                                  (2, 4, '10:00', '18:00'),
                                                                                  (2, 5, '10:00', '18:00');