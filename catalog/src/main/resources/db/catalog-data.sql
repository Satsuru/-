INSERT INTO catalog.hotels (id, name, city, address, star_rating) VALUES
    (1, 'Неман', 'Минск', 'ул. Кирова, 1', 4.0);

INSERT INTO catalog.rooms (id, hotel_id, room_type, capacity, base_price, active) VALUES
    (1, 1, 'Стандарт', 2, 100.00, TRUE),
    (2, 1, 'Люкс', 3, 180.00, TRUE);

INSERT INTO catalog.room_nights (room_id, stay_date, state, booking_id) VALUES
    (1, DATE '2026-06-10', 'HELD', 1),
    (1, DATE '2026-06-11', 'HELD', 1),
    (2, DATE '2026-06-01', 'SOLD', 2);
