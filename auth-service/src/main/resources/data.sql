INSERT IGNORE INTO roles (id, name) VALUES
  (1, 'ADMIN'),
  (2, 'FOUNDER'),
  (3, 'INVESTOR'),
  (4, 'COFOUNDER');

-- BCrypt hash for plain password: password
INSERT IGNORE INTO users (id, name, email, password_hash, created_at) VALUES
  (1, 'Default Admin', 'admin@founderlink.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi5M9Y1Q6ttuPAoBs64K6TfGsDz3jHK', NOW()),
  (2, 'Seed Founder', 'seed.founder@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi5M9Y1Q6ttuPAoBs64K6TfGsDz3jHK', NOW()),
  (3, 'Seed Investor', 'seed.investor@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi5M9Y1Q6ttuPAoBs64K6TfGsDz3jHK', NOW()),
  (4, 'Seed CoFounder', 'seed.cofounder@example.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi5M9Y1Q6ttuPAoBs64K6TfGsDz3jHK', NOW());

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (4, 4);
