CREATE SCHEMA IF NOT EXISTS pricing;

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
