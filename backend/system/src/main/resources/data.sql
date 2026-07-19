INSERT INTO system.roles (code, name)
VALUES
    ('ADMIN', 'Administrator'),
    ('STUDENT', 'Student'),
    ('LECTURER', 'Lecturer')
ON CONFLICT (code) DO NOTHING;

INSERT INTO system."user"
    (id, email, password, full_name, avatar, role_id, created_at, updated_at, deleted)
SELECT
    gen_random_uuid(),
    'admin@gmail.com',
    '$2a$10$geQo4kEjV22SLPDoKb80YOWdj8csdZ/qEPSoV2KsFVyX7DEEuc80i',
    'Administrator',
    NULL,
    role.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM system.roles role
WHERE role.code = 'ADMIN'
ON CONFLICT (email) DO NOTHING;
