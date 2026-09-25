ALTER TABLE core.course_chapter
DROP CONSTRAINT IF EXISTS uk_course_chapter_order;

ALTER TABLE core.course_topic
DROP CONSTRAINT IF EXISTS uk_chapter_topic_order;

CREATE UNIQUE INDEX IF NOT EXISTS uk_course_chapter_order_active
    ON core.course_chapter (course_id, order_index)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_chapter_topic_order_active
    ON core.course_topic (chapter_id, order_index)
    WHERE deleted_at IS NULL;