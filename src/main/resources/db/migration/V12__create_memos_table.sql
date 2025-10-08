CREATE TABLE memos (
                       id BIGSERIAL PRIMARY KEY,
                       project_id BIGINT NOT NULL,
                       memo_date DATE NOT NULL,
                       content TEXT NOT NULL,
                       author_id BIGINT,
                       created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_memos_project
                           FOREIGN KEY (project_id)
                               REFERENCES projects(id)
                               ON UPDATE CASCADE
                               ON DELETE CASCADE,
                       CONSTRAINT fk_memos_author
                           FOREIGN KEY (author_id)
                               REFERENCES users(id)
                               ON UPDATE CASCADE
                               ON DELETE SET NULL
);

-- 인덱스 생성
CREATE INDEX idx_memos_project_date
    ON memos (project_id, memo_date);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION update_memos_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_memos_updated_at
    BEFORE UPDATE ON memos
    FOR EACH ROW
    EXECUTE FUNCTION update_memos_updated_at();
