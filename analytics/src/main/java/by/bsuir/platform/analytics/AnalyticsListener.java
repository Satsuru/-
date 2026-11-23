package by.bsuir.platform.analytics;

import by.bsuir.platform.contracts.BusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

import static by.bsuir.platform.contracts.Topics.BOOKING_CREATED;
import static by.bsuir.platform.contracts.Topics.BOOKING_PAID;

/** После оплаты дописывает факты продаж и занятых ночей. */
@Component
class AnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsListener.class);
    private final AnalyticsService analytics;

    AnalyticsListener(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @KafkaListener(topics = {BOOKING_CREATED, BOOKING_PAID})
    public void onEvent(String payload) {
        BusEvent event = BusEvent.parse(payload);
        if (BusEvent.BOOKING_CREATED.equals(event.type())) {
            analytics.addSale(event.hotelId(), "CREATED", new BigDecimal(event.amount()), LocalDate.parse(event.checkIn()));
        } else if (BusEvent.BOOKING_PAID.equals(event.type())) {
            LocalDate checkIn = LocalDate.parse(event.checkIn());
            analytics.addSale(event.hotelId(), "PAID", new BigDecimal(event.amount()), checkIn);
            analytics.recordSoldNights(event.hotelId(), checkIn, LocalDate.parse(event.checkOut()), event.totalRooms());
        } else {
            log.warn("Неизвестное событие {}", event.type());
        }
    }
}
