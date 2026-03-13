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