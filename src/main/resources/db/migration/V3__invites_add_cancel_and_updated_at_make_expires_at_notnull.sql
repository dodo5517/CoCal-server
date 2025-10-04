-- V3: invites 스키마 변경
-- 변경점
--  1) status CHECK 제약에 'CANCEL' 추가
--  2) updated_at TIMESTAMP(6) NOT NULL DEFAULT current_timestamp(6) 추가
--  3) expires_at NOT NULL 강제 (기존 NULL 값 백필)

BEGIN;

-- 1) updated_at 컬럼 추가 + 값 채우고 NOT NULL + DEFAULT 부여
ALTER TABLE invites
    ADD COLUMN IF NOT EXISTS updated_at timestamp(6);

UPDATE invites
SET updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP(6));

ALTER TABLE invites
    ALTER COLUMN updated_at SET NOT NULL,
ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);

-- 2) expires_at NOT NULL 강제 전, NULL 값 백필
--    정책: created_at + 7일. 필요 시 기간 변경 가능.
UPDATE invites
SET expires_at = created_at + INTERVAL '7 days'
WHERE expires_at IS NULL;

ALTER TABLE invites
    ALTER COLUMN expires_at SET NOT NULL;

-- 3) status CHECK 제약을 'CANCEL' 포함으로 교체
--    기존 CHECK 제약 이름은 자동 생성일 수 있으므로 동적으로 찾아 삭제 후 새로 추가
DO $$
DECLARE
cname text;
BEGIN
SELECT con.conname INTO cname
FROM pg_constraint con
WHERE con.conrelid = 'invites'::regclass
    AND con.contype = 'c'
    AND pg_get_constraintdef(con.oid) ILIKE '%status%'
  LIMIT 1;

IF cname IS NOT NULL THEN
    EXECUTE format('ALTER TABLE invites DROP CONSTRAINT %I', cname);
END IF;
END $$;

ALTER TABLE invites
    ADD CONSTRAINT invites_status_check
        CHECK (status IN ('PENDING','ACCEPTED','DECLINED','EXPIRED','CANCEL'));
