-- Seed SUPERADMIN user (BCrypt of "superadmin123")
INSERT IGNORE INTO users (email, phone_number, password, role, auth_provider, email_verified, created_at)
VALUES (
    'superadmin@rapiffy.com',
    '8490946308',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lheO',
    'SUPER_ADMIN',
    'NORMAL',
    true,
    NOW()
);
