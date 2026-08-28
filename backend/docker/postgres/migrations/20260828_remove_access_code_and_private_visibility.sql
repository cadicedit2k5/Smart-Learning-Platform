-- Chạy một lần trên database hiện có trước khi deploy phiên bản backend này.
-- Dự án chưa dùng Flyway nên script không được ứng dụng tự động thực thi.
BEGIN;

UPDATE core.course
SET visibility = 'INVITE_ONLY'
WHERE visibility = 'PRIVATE';

ALTER TABLE core.course
    ALTER COLUMN visibility SET DEFAULT 'INVITE_ONLY';

DROP TABLE IF EXISTS core.access_code;

COMMIT;
