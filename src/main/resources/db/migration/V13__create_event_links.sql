-- updated_at 자동 세터 함수 (존재 확인 후 생성)
DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM pg_proc p
      JOIN pg_namespace n ON n.oid = p.pronamespace
      WHERE p.proname = 'set_updated_at' AND n.nspname = 'public'
  ) THEN
CREATE FUNCTION set_updated_at() RETURNS trigger AS $f$
BEGIN
      NEW.updated_at = now();
RETURN NEW;
END;
    $f$ LANGUAGE plpgsql;
END IF;
END$$;

-- 테이블 생성
CREATE TABLE IF NOT EXISTS event_links (
                                           id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                           event_id    BIGINT            NOT NULL,
                                           url         VARCHAR(2000)     NOT NULL,
    title       VARCHAR(255)      NULL,
    order_no    INT               NOT NULL DEFAULT 0,   -- UI 정렬용
    created_by  BIGINT            NULL,                 -- 작성자 (nullable: 탈퇴 시 SET NULL)
    created_at  TIMESTAMPTZ(6)    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ(6)    NOT NULL DEFAULT now(),

    -- 무결성/중복 방지
    CONSTRAINT fk_el_event
    FOREIGN KEY (event_id) REFERENCES events(id)
    ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_el_creator
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON UPDATE CASCADE ON DELETE SET NULL,

    CONSTRAINT uk_el_event_url UNIQUE (event_id, url),
    CONSTRAINT ck_el_order_no CHECK (order_no >= 0)
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_el_event        ON event_links(event_id);
CREATE INDEX IF NOT EXISTS idx_el_event_order  ON event_links(event_id, order_no);

-- updated_at 자동 갱신 트리거
DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM pg_trigger WHERE tgname = 'trg_event_links_updated_at'
  ) THEN
CREATE TRIGGER trg_event_links_updated_at
    BEFORE UPDATE ON event_links
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END$$;
