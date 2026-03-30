INSERT IGNORE INTO users (id, user_type, name, email, role, created_at, updated_at) VALUES
  (1, 'ADMIN',     'Default Admin',  'admin@founderlink.local',    'ROLE_ADMIN',    NOW(), NOW()),
  (2, 'FOUNDER',   'Seed Founder',   'seed.founder@example.com',   'ROLE_FOUNDER',  NOW(), NOW()),
  (3, 'INVESTOR',  'Seed Investor',  'seed.investor@example.com',  'ROLE_INVESTOR', NOW(), NOW()),
  (4, 'COFOUNDER', 'Seed CoFounder', 'seed.cofounder@example.com', 'ROLE_COFUNDER', NOW(), NOW());

INSERT IGNORE INTO admin (id) VALUES (1);

INSERT IGNORE INTO founder (id, startup_name, industry, funding_goal) VALUES
  (2, 'Seed Startup', 'SaaS', 100000.00);

INSERT IGNORE INTO investor (id, investment_budget, preferred_industries) VALUES
  (3, 500000.00, 'SaaS,FinTech');

INSERT IGNORE INTO co_founder (id, expertise, experience) VALUES
  (4, 'Backend', '5 years');

INSERT IGNORE INTO user_profiles (id, user_id, skills, experience, bio, portfolio_link, created_at, updated_at) VALUES
  (1, 1, 'Governance',   '10 years', 'Seed admin profile', NULL, NOW(), NOW()),
  (2, 2, 'Java,Spring',  '7 years',  'Seed founder profile', 'https://example.com/founder', NOW(), NOW()),
  (3, 3, 'Funding,VC',   '8 years',  'Seed investor profile', 'https://example.com/investor', NOW(), NOW()),
  (4, 4, 'Architecture', '5 years',  'Seed cofounder profile', 'https://example.com/cofounder', NOW(), NOW());
