-- 프로젝트 멤버 테이블 생성 (PostgreSQL)
CREATE TABLE project_members (
                                 id BIGSERIAL PRIMARY KEY,

                                 project_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,

                                 role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
                                 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_pm_project FOREIGN KEY (project_id)
                                     REFERENCES projects (id)
                                     ON UPDATE CASCADE
                                     ON DELETE CASCADE,

                                 CONSTRAINT fk_pm_user FOREIGN KEY (user_id)
                                     REFERENCES users (id)
                                     ON UPDATE CASCADE
                                     ON DELETE CASCADE
);

-- 인덱스 설정
CREATE INDEX idx_pm_project ON project_members (project_id);
CREATE INDEX idx_pm_user ON project_members (user_id);

-- role, status 체크 제약조건 추가
ALTER TABLE project_members
    ADD CONSTRAINT chk_pm_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    ADD CONSTRAINT chk_pm_status CHECK (status IN ('ACTIVE', 'LEFT', 'KICKED', 'BLOCKED'));

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION update_project_members_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_project_members_updated_at
    BEFORE UPDATE ON project_members
    FOR EACH ROW
    EXECUTE FUNCTION update_project_members_updated_at();
