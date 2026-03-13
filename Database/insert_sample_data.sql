USE checkers_db;

-- Clear existing data from tables (respecting foreign key constraints)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE matches;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert sample data into users table (IDs will be auto-generated as 1, 2, 3, 4, 5)
INSERT INTO users (username, password_hash, age_group, created_at, chat_enabled, color_scheme, skill_rating) VALUES
('player1', MD5('123456'), 'adult', '2026-02-15 10:30:00', TRUE, 'default', 1200),  -- user_id = 1
('player2', MD5('aabbcc'), 'adult', '2026-02-16 14:20:00', TRUE, 'colorblind', 1100),  -- user_id = 2
('younggamer', MD5('djfdka32'), 'child', '2026-02-17 09:15:00', FALSE, 'default', 800),  -- user_id = 3
('pro_player', MD5('aa23#'), 'adult', '2026-02-18 16:45:00', TRUE, 'default', 1500),  -- user_id = 4
('newbie', MD5('kaf394d'), 'child', '2026-02-19 11:30:00', FALSE, 'colorblind', 500);  -- user_id = 5

-- Insert sample data into matches table (references the above user IDs)
INSERT INTO matches (player1_id, player2_id, start_time, end_time, status, winner_id) VALUES
(1, 2, '2026-02-20 10:00:00', '2026-02-20 10:10:00', 'completed', 1),
(3, 5, '2026-02-20 11:00:00', '2026-02-20 11:08:00', 'completed', 3),
(2, 5, '2026-02-20 13:00:00', '2026-02-20 13:15:00', 'completed', 2),
(3, 4, '2026-02-20 14:00:00', NULL, 'in_progress', NULL);
