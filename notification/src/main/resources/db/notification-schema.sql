CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notifications (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    kind VARCHAR(40) NOT NULL,
    body VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
