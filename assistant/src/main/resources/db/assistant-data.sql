INSERT INTO assistant.dialogs (id, user_id, status) VALUES
    (1, 1, 'OPEN');

INSERT INTO assistant.messages (id, dialog_id, author, body) VALUES
    (1, 1, 'GUEST', 'Сколько стоит стандарт в июне?'),
    (2, 1, 'ASSISTANT', 'Цена считается по правилам отеля на даты заезда.');
