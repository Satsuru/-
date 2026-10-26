CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payments (
    id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_ref VARCHAR(80)
);
