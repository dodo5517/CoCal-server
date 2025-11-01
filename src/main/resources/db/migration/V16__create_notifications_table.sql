-- V16__create_notifications_table.sql
CREATE TABLE notifications (
   id BIGSERIAL PRIMARY KEY,           -- 자동 증가 ID
   user_id BIGINT NOT NULL,            -- 알림 받을 대상
   type VARCHAR(50) NOT NULL,          -- INVITE / EVENT / TODO
   reference_id BIGINT,                -- 관련 대상 ID (초대 ID, 이벤트 ID, Todo ID 등)
   title VARCHAR(255),                 -- 알림 제목
   message TEXT,                       -- 알림 내용
   sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 생성 시간
   is_read BOOLEAN DEFAULT FALSE       -- 읽음 여부
);

