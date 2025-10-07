-- v11: 개인 TODO 테이블
CREATE TABLE private_todos (
                               id BIGSERIAL PRIMARY KEY,
                               project_id BIGINT NOT NULL,
                               owner_id BIGINT NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               description TEXT,
                               date TIMESTAMP(6),
                               url VARCHAR(2048),
                               status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                                   CHECK (status IN ('IN_PROGRESS','DONE')),
                               offset_minutes INT NOT NULL DEFAULT 0,
                               order_no INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_pt_project FOREIGN KEY (project_id) REFERENCES projects(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE,
                               CONSTRAINT fk_pt_owner FOREIGN KEY (owner_id) REFERENCES users(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_pt_owner_due ON private_todos(owner_id, date);
CREATE INDEX idx_pt_project ON private_todos(project_id);

-- v11: 이벤트 종속 TODO 테이블
CREATE TABLE event_todos (
                             id BIGSERIAL PRIMARY KEY,
                             event_id BIGINT NOT NULL,
                             title VARCHAR(200) NOT NULL,
                             description TEXT,
                             url VARCHAR(2048),
                             status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                                 CHECK (status IN ('IN_PROGRESS','DONE')),
                             offset_minutes INT NOT NULL DEFAULT 0,
                             author_id BIGINT,
                             order_no INT NOT NULL DEFAULT 0,
                             created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                             CONSTRAINT fk_et_event FOREIGN KEY (event_id) REFERENCES events(id)
                                 ON UPDATE CASCADE ON DELETE CASCADE,
                             CONSTRAINT fk_et_assignee FOREIGN KEY (author_id) REFERENCES users(id)
                                 ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_et_event ON event_todos(event_id);
CREATE INDEX idx_et_assignee ON event_todos(author_id);
