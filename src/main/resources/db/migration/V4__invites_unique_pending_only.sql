-- 1) 기존 유니크 제약 삭제
ALTER TABLE invites DROP CONSTRAINT IF EXISTS uk_invite_project_email;

-- 2) 활성(PENDING)만 유니크: 부분 유니크 인덱스
--    (citext를 썼다면 lower() 없이 email 그대로 사용 가능)
CREATE UNIQUE INDEX IF NOT EXISTS ux_invites_project_email_pending
    ON invites (project_id, email)
    WHERE status = 'PENDING';
