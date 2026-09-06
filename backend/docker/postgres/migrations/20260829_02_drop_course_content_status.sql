-- Chỉ chạy sau khi toàn bộ instance backend cũ đã dừng và backend mới đã được
-- kiểm tra ổn định. Dự án chưa dùng Flyway nên script không tự động chạy.
BEGIN;

ALTER TABLE core.course_chapter
    DROP COLUMN status;

ALTER TABLE core.course_topic
    DROP COLUMN status;

COMMIT;
