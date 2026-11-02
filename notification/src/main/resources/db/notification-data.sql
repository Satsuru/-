INSERT INTO notification.notifications (id, user_id, kind, body, status, created_at) VALUES
    (1, 1, 'BOOKING_CREATED', 'Бронь 1 создана, номер удерживается до оплаты', 'NEW', TIMESTAMP '2026-06-01 10:00:00'),
    (2, 1, 'PAYMENT_COMPLETED', 'Бронь 2 оплачена', 'READ', TIMESTAMP '2026-06-01 12:00:00');
