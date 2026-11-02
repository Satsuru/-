-- Бронь 1: 2 ночи, 100 * 1.3 * 1.2 * 1 * 1 * 1 = 156 за ночь, 312 за проживание.
-- Удержание номера на эти ночи лежит в catalog.room_nights.
-- Срок оплаты сдвинут на 31 декабря 2026, чтобы учебная бронь оставалась CREATED после июня.
-- Бронь 2: 1 ночь, 180 * 1.3 * 1.2 * 1 * 1.1 * 1 = 308.88. Ночь продана в каталоге.
INSERT INTO booking.bookings (id, user_id, room_id, hotel_id, check_in, check_out, guests, status, total_price, hold_until) VALUES
    (1, 1, 1, 1, DATE '2026-06-10', DATE '2026-06-12', 2, 'CREATED', 312.00, TIMESTAMP '2026-12-31 18:00:00'),
    (2, 1, 2, 1, DATE '2026-06-01', DATE '2026-06-02', 2, 'PAID', 308.88, NULL);

INSERT INTO booking.price_snapshots (id, booking_id, base_price, occupancy_multiplier, season_multiplier, weekday_multiplier, demand_multiplier, guest_multiplier, calculated_price) VALUES
    (1, 1, 100.00, 1.300, 1.200, 1.000, 1.000, 1.000, 312.00),
    (2, 2, 180.00, 1.300, 1.200, 1.000, 1.100, 1.000, 308.88);
