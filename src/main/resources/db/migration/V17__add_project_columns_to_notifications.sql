-- V17__add_project_columns_to_notifications.sql
-- 기존 notifications 테이블에 project_id, project_name 컬럼 추가

ALTER TABLE notifications
    ADD COLUMN project_id BIGINT,
ADD COLUMN project_name VARCHAR(255);

-- 필요 시: project_id에 외래키 추가 (Project 테이블이 존재한다면)
ALTER TABLE notifications
ADD CONSTRAINT fk_notifications_project
FOREIGN KEY (project_id) REFERENCES projects(id)
ON DELETE SET NULL;
