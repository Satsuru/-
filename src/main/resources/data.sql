INSERT INTO identity.users (id, email, password_hash, full_name, phone, active) VALUES
    (1, 'guest@hotel.local', 'not-a-real-hash', 'Анна Гость', '+375291112233', TRUE),
    (2, 'worker@hotel.local', 'not-a-real-hash', 'Иван Сотрудник', NULL, TRUE);

INSERT INTO identity.user_roles (id, user_id, role_code) VALUES
    (1, 1, 'GUEST'),
    (2, 2, 'WORKER');

INSERT INTO catalog.hotels (id, name, city, address, star_rating) VALUES
    (1, 'Неман', 'Минск', 'ул. Кирова, 1', 4.0);

INSERT INTO catalog.rooms (id, hotel_id, room_type, capacity, base_price, active) VALUES
    (1, 1, 'Стандарт', 2, 100.00, TRUE),
    (2, 1, 'Люкс', 3, 180.00, TRUE);

INSERT INTO catalog.room_nights (room_id, stay_date, state, booking_id) VALUES
    (2, DATE '2026-06-01', 'SOLD', 2);

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

INSERT INTO booking.bookings (id, user_id, room_id, hotel_id, check_in, check_out, guests, status, total_price, hold_until) VALUES
    (1, 1, 1, 1, DATE '2026-06-10', DATE '2026-06-12', 2, 'CREATED', 264.00, TIMESTAMP '2026-06-09 18:00:00'),
    (2, 1, 2, 1, DATE '2026-05-01', DATE '2026-05-03', 2, 'PAID', 300.00, NULL);

INSERT INTO booking.price_snapshots (id, booking_id, base_price, occupancy_multiplier, season_multiplier, weekday_multiplier, demand_multiplier, guest_multiplier, calculated_price) VALUES
    (1, 1, 100.00, 1.000, 1.200, 1.000, 1.100, 1.000, 264.00),
    (2, 2, 180.00, 1.000, 1.000, 1.000, 1.000, 1.000, 300.00);

INSERT INTO payment.payments (id, booking_id, user_id, amount, method, status, provider_ref) VALUES
    (1, 2, 1, 300.00, 'CARD', 'COMPLETED', 'pay-2');

INSERT INTO assistant.dialogs (id, user_id, status) VALUES
    (1, 1, 'OPEN');

INSERT INTO assistant.messages (id, dialog_id, author, body) VALUES
    (1, 1, 'GUEST', 'Сколько стоит стандарт в июне?'),
    (2, 1, 'ASSISTANT', 'Цена считается по правилам отеля на даты заезда.');

INSERT INTO notification.notifications (id, user_id, kind, body, status, created_at) VALUES
    (1, 1, 'BOOKING_CREATED', 'Бронь 1 создана, номер удерживается до оплаты', 'NEW', TIMESTAMP '2026-06-01 10:00:00'),
    (2, 1, 'PAYMENT_COMPLETED', 'Бронь 2 оплачена', 'READ', TIMESTAMP '2026-05-01 12:00:00');

INSERT INTO analytics.sales_facts (id, hotel_id, status, amount, occurred_on) VALUES
    (1, 1, 'PAID', 300.00, DATE '2026-05-02'),
    (2, 1, 'CREATED', 264.00, DATE '2026-06-10');

INSERT INTO analytics.occupancy_facts (id, hotel_id, stay_date, sold_nights, total_nights) VALUES
    (1, 1, DATE '2026-05-01', 1, 2),
    (2, 1, DATE '2026-06-01', 1, 2);
