CREATE SCHEMA IF NOT EXISTS booking;

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
