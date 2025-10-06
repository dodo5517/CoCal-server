-- V9__modify_events_table.sql
-- 1) 기존 url_id 삭제
ALTER TABLE events
DROP COLUMN url_id;

-- 2) url 문자열 컬럼 추가
ALTER TABLE events
    ADD COLUMN url VARCHAR(2048) NULL;

-- 3) visibility 컬럼을 ENUM 대신 VARCHAR로 변경
ALTER TABLE events
ALTER COLUMN visibility TYPE VARCHAR(10) USING visibility::VARCHAR,
ALTER COLUMN visibility SET DEFAULT 'PUBLIC',
ADD CONSTRAINT chk_visibility CHECK (visibility IN ('PRIVATE','PUBLIC'));
