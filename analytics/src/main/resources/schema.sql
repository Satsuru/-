CREATE SCHEMA IF NOT EXISTS analytics;

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
