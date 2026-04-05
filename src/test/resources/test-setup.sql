DELETE FROM subscription;
DELETE FROM location;
DELETE FROM users;
INSERT INTO users (id, email, password) VALUES ('00000000-0000-0000-0000-000000000001', 'test@example.com', 'hashed-password');
