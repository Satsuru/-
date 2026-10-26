CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.hotels (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(300),
    star_rating DOUBLE NOT NULL
);

CREATE TABLE catalog.rooms (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_type VARCHAR(80) NOT NULL,
    capacity INT NOT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE catalog.room_nights (
    room_id BIGINT NOT NULL,
    stay_date DATE NOT NULL,
    state VARCHAR(20) NOT NULL,
    booking_id BIGINT,
    PRIMARY KEY (room_id, stay_date)
);
