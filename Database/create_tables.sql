-- Create database 
CREATE DATABASE IF NOT EXISTS checkers_db;
USE checkers_db;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(40) NOT NULL,
    -- Age group must be either child or adult
    age_group VARCHAR(10) NOT NULL CHECK (age_group IN ('child', 'adult')),
    created_at TIMESTAMP NOT NULL,
    chat_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Color scheme must be default or colorblind
    color_scheme VARCHAR(15) NOT NULL DEFAULT 'default' CHECK (color_scheme IN ('default', 'colorblind')),
    -- Skill rating cannot be negative
    skill_rating INTEGER NOT NULL DEFAULT 0 CHECK (skill_rating >= 0)
);

-- Create matches table
CREATE TABLE IF NOT EXISTS matches (
    match_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    player1_id INTEGER NOT NULL,
    player2_id INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    -- Status must be in_progress or completed
    status VARCHAR(15) NOT NULL DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'completed')),
    winner_id INTEGER NULL,
    FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(user_id),
    -- Ensure two different players in a match
    CHECK (player1_id != player2_id),
    -- End time must be after start time if provided
    CHECK (end_time IS NULL OR end_time > start_time),
    -- Winner must be one of the players in the match
    CHECK (winner_id IS NULL OR winner_id = player1_id OR winner_id = player2_id)
);

-- Create moves table
CREATE TABLE IF NOT EXISTS moves (
    move_id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    match_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    from_row INTEGER NOT NULL CHECK (from_row >= 1 and from_row <= 8),
    from_col INTEGER NOT NULL CHECK (from_col >= 1 and from_col <= 8),
    to_row INTEGER NOT NULL CHECK (to_row >= 1 and to_row <= 8),
    to_col INTEGER NOT NULL CHECK (to_col >= 1 and to_col <= 8),
    move_seq INTEGER NOT NULL CHECK (move_seq >= 1),
    move_timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (match_id) REFERENCES matches(match_id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create player_stats table
CREATE TABLE IF NOT EXISTS player_stats (
    stat_id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    player_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    outcome VARCHAR(10) NOT NULL CHECK (outcome IN ('win', 'loss', 'draw')),
    skill_rating_change INTEGER NOT NULL,
	FOREIGN KEY (match_id) REFERENCES matches(match_id) ON DELETE CASCADE,
	FOREIGN KEY (player_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create match_rooms table
CREATE TABLE IF NOT EXISTS match_rooms (
    room_id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    match_id INTEGER,
    chat_enabled BOOLEAN NOT NULL DEFAULT true,
	FOREIGN KEY (match_id) REFERENCES matches(match_id) ON DELETE CASCADE
);

-- Create chat_histories table
CREATE TABLE IF NOT EXISTS chat_histories (
    chat_id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    room_id INTEGER NOT NULL,
    sender INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    content TEXT NOT NULL,
	FOREIGN KEY (room_id) REFERENCES match_rooms(room_id) ON DELETE CASCADE,
	FOREIGN KEY (sender) REFERENCES users(user_id) ON DELETE CASCADE
);
