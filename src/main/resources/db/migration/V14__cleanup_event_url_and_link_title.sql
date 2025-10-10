-- event_links.title 제거
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'event_links' AND column_name = 'title'
    ) THEN
ALTER TABLE event_links
DROP COLUMN title;
END IF;
END$$;

-- events.url 제거
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'events' AND column_name = 'url'
    ) THEN
ALTER TABLE events
DROP COLUMN url;
END IF;
END$$;