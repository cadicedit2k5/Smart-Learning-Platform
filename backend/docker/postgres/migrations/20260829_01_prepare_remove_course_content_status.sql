-- Chạy một lần trên database hiện có trước khi deploy backend không còn
-- CourseContentStatus. Dự án chưa dùng Flyway nên script không tự động chạy.
-- Script giữ cột status tương thích với instance backend cũ trong lúc rollout.
BEGIN;

-- Chuyển trạng thái ARCHIVED sang cơ chế soft delete duy nhất là deleted_at.
UPDATE core.course_chapter
SET deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP)
WHERE status = 'ARCHIVED';

-- Xóa mềm cả topic ARCHIVED và topic thuộc chapter đã bị xóa.
UPDATE core.course_topic AS topic
SET deleted_at = COALESCE(topic.deleted_at, chapter.deleted_at, CURRENT_TIMESTAMP)
FROM core.course_chapter AS chapter
WHERE topic.chapter_id = chapter.id
  AND topic.deleted_at IS NULL
  AND (
      topic.status = 'ARCHIVED'
      OR chapter.deleted_at IS NOT NULL
  );

-- Trong giai đoạn tương thích, mọi nội dung còn hoạt động được coi là PUBLISHED.
-- Course.status sẽ quyết định khóa học có được công khai hay không.
UPDATE core.course_chapter
SET status = 'PUBLISHED'
WHERE deleted_at IS NULL;

UPDATE core.course_topic
SET status = 'PUBLISHED'
WHERE deleted_at IS NULL;

-- Backend mới không ghi cột status. Default này cho phép giữ cột trong lúc
-- rollout mà không làm insert mới vi phạm ràng buộc NOT NULL.
ALTER TABLE core.course_chapter
    ALTER COLUMN status SET DEFAULT 'PUBLISHED';

ALTER TABLE core.course_topic
    ALTER COLUMN status SET DEFAULT 'PUBLISHED';

COMMIT;
