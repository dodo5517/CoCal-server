-- V7__create_events_table.sql
-- PostgreSQL용 events 테이블

-- 1) ENUM 타입 생성
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'visibility_enum') THEN
CREATE TYPE visibility_enum AS ENUM ('PRIVATE','PUBLIC');
END IF;
END $$;

-- 2) events 테이블 생성
CREATE TABLE events (
                        id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                        project_id BIGINT NOT NULL,
                        url_id BIGINT NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        description TEXT,
                        start_at TIMESTAMP(6) NOT NULL,
                        end_at TIMESTAMP(6) NOT NULL,
                        all_day BOOLEAN NOT NULL DEFAULT FALSE,
                        visibility visibility_enum NOT NULL DEFAULT 'PUBLIC',
                        author_id BIGINT NOT NULL,
                        location VARCHAR(255),
                        offset_minutes INTEGER NOT NULL DEFAULT 0,
                        color VARCHAR(7) NOT NULL DEFAULT '#0B3559',
                        created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        CHECK (end_at > start_at),
                        CONSTRAINT fk_ev_project FOREIGN KEY (project_id) REFERENCES projects(id) ON UPDATE CASCADE ON DELETE CASCADE,
                        CONSTRAINT fk_ev_author FOREIGN KEY (author_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE SET NULL
);

-- 인덱스
CREATE INDEX idx_ev_project_time ON events(project_id, start_at, end_at);
CREATE INDEX idx_ev_author ON events(author_id);
