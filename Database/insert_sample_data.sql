USE checkers_db;

-- Clear existing data from tables (respecting foreign key constraints)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE moves;
TRUNCATE TABLE matches;
TRUNCATE TABLE chat_histories;
TRUNCATE TABLE match_rooms;
TRUNCATE TABLE player_stats;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert sample data into users table (IDs will be auto-generated as 1, 2, 3, 4, 5)
INSERT INTO users (username, password_hash, age_group, created_at, chat_enabled, color_scheme, skill_rating) VALUES
('player1', MD5('123456'), 'adult', '2026-02-15 10:30:00', TRUE, 'default', 1200),  -- user_id = 1
('player2', MD5('aabbcc'), 'adult', '2026-02-16 14:20:00', TRUE, 'colorblind', 1100),  -- user_id = 2
('younggamer', MD5('djfdka32'), 'child', '2026-02-17 09:15:00', FALSE, 'default', 800);  -- user_id = 3

-- Insert sample data into matches table (references the above user IDs)
INSERT INTO matches (player1_id, player2_id, start_time, end_time, status, winner_id) VALUES
(1, 2, '2026-02-20 09:59:54', NULL, 'in_progress', NULL);

-- Insert sample data into moves table (based on the matches already created)
INSERT INTO moves (match_id, player_id, from_row, from_col, to_row, to_col, move_seq, move_timestamp) VALUES
(1, 1, 6, 1, 5, 2, 1, '2026-02-20 10:00:00');

-- Insert sample data into match_rooms table (one room for each match)
INSERT INTO match_rooms (created_at, is_active, match_id, chat_enabled) VALUES
('2026-02-20 09:59:50', FALSE, 1, TRUE);

-- Insert sample data into chat_histories table (chats in the match rooms)
INSERT INTO chat_histories (room_id, sender, created_at, content) VALUES
(1, 1, '2026-02-20 10:00:30', 'Good luck!'),
(1, 2, '2026-02-20 10:00:45', 'You too!');
