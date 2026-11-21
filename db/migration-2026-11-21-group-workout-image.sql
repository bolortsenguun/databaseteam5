-- 2026-11-21 그룹 수업 운동 종류·사진 컬럼 추가 (PR #21)
-- 이미 schema.sql로 DB를 만든 경우 한 번만 실행 (새로 만드는 경우는 schema.sql에 포함되어 있어 필요 없음)
-- 실행하지 않으면 서버 시작 시 ddl-auto: validate 에서 workout / image_url 컬럼이 없다는 오류가 납니다.

ALTER TABLE group_classes
    ADD COLUMN workout   VARCHAR(20) NULL COMMENT '운동 종류(서킷/하체/상체/코어/전신/스트레칭/러닝/케틀벨/데드리프트/힙·브릿지), 선택'
        AFTER description,
    ADD COLUMN image_url MEDIUMTEXT  NULL COMMENT '수업 사진(data URL, 프론트에서 720px JPEG로 축소), 없으면 NULL'
        AFTER workout;
