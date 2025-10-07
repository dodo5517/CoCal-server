-- v11: 개인 TODO 테이블
CREATE TABLE private_todos (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               project_id BIGINT UNSIGNED NOT NULL,      -- 개인 TODO도 프로젝트 단위로 필터링 필요
                               owner_id BIGINT UNSIGNED NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               description TEXT NULL,
                               date DATETIME(6) NULL,                   -- todo 표시되는 날짜
                               url VARCHAR(2048) NULL,                  -- 링크 저장
                               status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                                   CHECK (status IN ('IN_PROGRESS','DONE')),
                               offset_minutes INT UNSIGNED NOT NULL DEFAULT 0,  -- 예: 15 → 15분 전
                               order_no INT NOT NULL DEFAULT 0,                -- 드래그 정렬용
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_pt_project FOREIGN KEY (project_id) REFERENCES projects(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE,
                               CONSTRAINT fk_pt_owner FOREIGN KEY (owner_id) REFERENCES users(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE,

                               KEY idx_pt_owner_due (owner_id, date),
                               KEY idx_pt_project (project_id)
);

-- v11: 이벤트 종속 TODO 테이블
CREATE TABLE event_todos (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             event_id BIGINT UNSIGNED NOT NULL,
                             title VARCHAR(200) NOT NULL,
                             description TEXT NULL,
                             url VARCHAR(2048) NULL,                  -- 링크 저장
                             status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                                 CHECK (status IN ('IN_PROGRESS','DONE')),
                             offset_minutes INT UNSIGNED NOT NULL DEFAULT 0,  -- 예: 15 → 15분 전
                             author_id BIGINT UNSIGNED NULL,                   -- 담당자(선택)
                             order_no INT NOT NULL DEFAULT 0,
                             created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                             CONSTRAINT fk_et_event FOREIGN KEY (event_id) REFERENCES events(id)
                                 ON UPDATE CASCADE ON DELETE CASCADE,
                             CONSTRAINT fk_et_assignee FOREIGN KEY (author_id) REFERENCES users(id)
                                 ON UPDATE CASCADE ON DELETE SET NULL,

                             KEY idx_et_event (event_id),
                             KEY idx_et_assignee (author_id)
);
