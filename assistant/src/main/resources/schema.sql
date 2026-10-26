CREATE SCHEMA IF NOT EXISTS assistant;

CREATE TABLE assistant.dialogs (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE assistant.messages (
    id BIGINT PRIMARY KEY,
    dialog_id BIGINT NOT NULL,
    author VARCHAR(20) NOT NULL,
    body VARCHAR(1000) NOT NULL
);
