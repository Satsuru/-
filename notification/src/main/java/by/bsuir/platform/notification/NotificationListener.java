package by.bsuir.platform.notification;

import by.bsuir.platform.contracts.BusEvent;
import by.bsuir.platform.contracts.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Слушает события брони и помощника и сохраняет уведомление в своей базе. */
@Component
class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final NotificationService notifications;

    NotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = {
            Topics.BOOKING_CREATED,
            Topics.BOOKING_PAID,
            Topics.BOOKING_CANCELLED,
            Topics.ASSISTANT_ESCALATED
    })
    public void onEvent(String payload) {
        BusEvent event = BusEvent.parse(payload);
        switch (event.type()) {
            case BusEvent.BOOKING_CREATED -> notifications.add(event.userId(), "BOOKING_CREATED",
                    "Бронь " + event.bookingId() + " создана, номер удерживается до оплаты");
            case BusEvent.BOOKING_PAID -> notifications.add(event.userId(), "PAYMENT_COMPLETED",
                    "Бронь " + event.bookingId() + " оплачена");
            case BusEvent.BOOKING_CANCELLED -> notifications.add(event.userId(), "BOOKING_CANCELLED",
                    "Бронь " + event.bookingId() + " отменена, номер снова свободен");
            case BusEvent.ASSISTANT_ESCALATED -> notifications.add(event.userId(), "ASSISTANT_ESCALATED",
                    "Гость ждёт сотрудника, диалог " + event.dialogId());
            default -> log.warn("Неизвестное событие {}", event.type());
        }
    }
}
