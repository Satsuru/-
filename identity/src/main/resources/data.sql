INSERT INTO identity.users (id, email, password_hash, full_name, phone, active) VALUES
    (1, 'guest@hotel.local', 'guest', 'Анна Гость', '+375291112233', TRUE),
    (2, 'worker@hotel.local', 'worker', 'Иван Сотрудник', NULL, TRUE);

INSERT INTO identity.user_roles (id, user_id, role_code) VALUES
    (1, 1, 'GUEST'),
    (2, 2, 'WORKER');
