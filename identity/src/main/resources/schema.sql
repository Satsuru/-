CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(200) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(40),
    active BOOLEAN NOT NULL
);

CREATE TABLE identity.user_roles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(40) NOT NULL
);

CREATE TABLE identity.access_tokens (
    token VARCHAR(80) PRIMARY KEY,
    user_id BIGINT NOT NULL
);
