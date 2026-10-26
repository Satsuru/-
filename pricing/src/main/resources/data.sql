INSERT INTO pricing.occupancy_rules (id, hotel_id, min_occupancy, max_occupancy, multiplier) VALUES
    (1, 1, 0.000, 0.500, 1.000),
    (2, 1, 0.500, 1.010, 1.300);

INSERT INTO pricing.seasonal_month_rules (id, hotel_id, month_number, multiplier) VALUES
    (1, 1, 1, 0.900),
    (2, 1, 6, 1.200);

INSERT INTO pricing.weekday_rules (id, hotel_id, day_of_week, multiplier) VALUES
    (1, 1, 1, 1.000),
    (2, 1, 2, 1.000),
    (3, 1, 3, 1.000),
    (4, 1, 4, 1.000),
    (5, 1, 5, 1.000),
    (6, 1, 6, 1.150),
    (7, 1, 7, 1.150);

INSERT INTO pricing.demand_rules (id, hotel_id, window_days, view_threshold, multiplier) VALUES
    (1, 1, 7, 20, 1.100);

INSERT INTO pricing.guest_rules (id, hotel_id, included_guests, extra_multiplier) VALUES
    (1, 1, 2, 0.100);

INSERT INTO pricing.hotel_view_stats (id, hotel_id, view_date, views) VALUES
    (1, 1, DATE '2026-05-28', 15),
    (2, 1, DATE '2026-05-30', 10);
