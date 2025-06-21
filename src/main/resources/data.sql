INSERT INTO fx_users (id, email, mobile, push_device_token, created_at, updated_at)
VALUES (RANDOM_UUID(), 'user1@example.com', '+447911123456', 'token_dev_abc123', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO fx_users (id, email, mobile, push_device_token, created_at, updated_at)
SELECT RANDOM_UUID(), 'user2@example.com', '+447911987654', 'token_dev_xyz789', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM fx_users WHERE email = 'user2@example.com');

INSERT INTO fx_users (id, email, mobile, push_device_token, created_at, updated_at)
SELECT RANDOM_UUID(), 'user3@example.com', '+447911555444', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM fx_users WHERE email = 'user3@example.com');

INSERT INTO fx_users (id, email, mobile, push_device_token, created_at, updated_at)
SELECT RANDOM_UUID(), 'testuser@mydomain.com', '+12125551234', 'token_dev_qwe000', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM fx_users WHERE email = 'testuser@mydomain.com');