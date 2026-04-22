ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NULL,
    ADD COLUMN password VARCHAR(100) NULL;

UPDATE users
SET username = CONCAT('legacy_user_', id)
WHERE username IS NULL;

UPDATE users
SET password = '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi8lT6Kv6Sleqrs9jwELyhl725LLJo.'
WHERE password IS NULL;

ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    MODIFY COLUMN password VARCHAR(100) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_username UNIQUE (username);
