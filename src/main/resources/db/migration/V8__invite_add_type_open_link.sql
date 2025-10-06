-- type 컬럼 추가: 기본값은 EMAIL (기존 데이터 호환)
ALTER TABLE invites
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'EMAIL';

-- 체크 제약: EMAIL 또는 OPEN_LINK 만 허용
ALTER TABLE invites
    ADD CONSTRAINT ck_invites_type CHECK (type IN ('EMAIL','OPEN_LINK'));

-- OPEN_LINK를 지원하려면 email을 NULL 허용으로 전환
ALTER TABLE invites
ALTER COLUMN email DROP NOT NULL;
