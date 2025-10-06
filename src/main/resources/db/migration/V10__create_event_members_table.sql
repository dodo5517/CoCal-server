CREATE TABLE event_members (
                               event_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT pk_event_members PRIMARY KEY (event_id, user_id),

                               CONSTRAINT fk_event_members_event FOREIGN KEY (event_id)
                                   REFERENCES events (id)
                                   ON UPDATE CASCADE
                                   ON DELETE CASCADE,

                               CONSTRAINT fk_event_members_user FOREIGN KEY (user_id)
                                   REFERENCES users (id)
                                   ON UPDATE CASCADE
                                   ON DELETE CASCADE
);

-- user_id에 인덱스 추가
CREATE INDEX idx_ep_user ON event_members (user_id);
