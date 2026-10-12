CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS pricing;
CREATE SCHEMA IF NOT EXISTS booking;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS assistant;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS analytics;

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

CREATE TABLE pricing.occupancy_rules (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    min_occupancy DECIMAL(6, 3) NOT NULL,
    max_occupancy DECIMAL(6, 3) NOT NULL,
    multiplier DECIMAL(8, 3) NOT NULL
);

CREATE TABLE pricing.seasonal_month_rules (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    month_number INT NOT NULL,
    multiplier DECIMAL(8, 3) NOT NULL
);

CREATE TABLE pricing.weekday_rules (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    multiplier DECIMAL(8, 3) NOT NULL
);

CREATE TABLE pricing.demand_rules (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    window_days INT NOT NULL,
    view_threshold INT NOT NULL,
    multiplier DECIMAL(8, 3) NOT NULL
);

CREATE TABLE pricing.guest_rules (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    included_guests INT NOT NULL,
    extra_multiplier DECIMAL(8, 3) NOT NULL
);

CREATE TABLE pricing.hotel_view_stats (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    view_date DATE NOT NULL,
    views INT NOT NULL
);

CREATE TABLE booking.bookings (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL,
    hold_until TIMESTAMP
);

CREATE TABLE booking.price_snapshots (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    base_price DECIMAL(12, 2) NOT NULL,
    occupancy_multiplier DECIMAL(8, 3) NOT NULL,
    season_multiplier DECIMAL(8, 3) NOT NULL,
    weekday_multiplier DECIMAL(8, 3) NOT NULL,
    demand_multiplier DECIMAL(8, 3) NOT NULL,
    guest_multiplier DECIMAL(8, 3) NOT NULL,
    calculated_price DECIMAL(12, 2) NOT NULL
);

CREATE TABLE payment.payments (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_ref VARCHAR(80)
);

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

CREATE TABLE notification.notifications (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    kind VARCHAR(40) NOT NULL,
    body VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE analytics.sales_facts (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    occurred_on DATE NOT NULL
);

CREATE TABLE analytics.occupancy_facts (
    id BIGINT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    stay_date DATE NOT NULL,
    sold_nights INT NOT NULL,
    total_nights INT NOT NULL
);
