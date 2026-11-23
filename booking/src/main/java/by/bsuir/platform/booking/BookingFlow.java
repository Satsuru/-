package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.BusEvent;
import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.Failures;
import by.bsuir.platform.contracts.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сценарий брони: каталог, цена и оплата вызываются по gRPC.
 * Уведомление и аналитика получают событие через Kafka.
 */
@Service
public class BookingFlow {

    private static final Logger log = LoggerFactory.getLogger(BookingFlow.class);
    private final BookingService bookings;
    private final BookingRemotes remotes;
    private final ObjectProvider<KafkaTemplate<String, String>> kafka;

    public BookingFlow(BookingService bookings, BookingRemotes remotes, ObjectProvider<KafkaTemplate<String, String>> kafka) {
        this.bookings = bookings;
        this.remotes = remotes;
        this.kafka = kafka;
    }

    public Api.BookingView create(long userId, long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        sweep();
        Dates.requireStay(checkIn, checkOut, guests);
        Api.RoomCard room = remotes.room(roomId, checkIn, checkOut)
                .orElseThrow(() -> new Failures.NotFoundException("Номер не найден"));
        if (guests > room.capacity()) {
            throw new Failures.BadRequestException("В номере недостаточно мест");
        }
        Api.PriceQuote price = remotes.price(room.hotelId(), room.basePrice(), room.occupancyRatio(), checkIn, checkOut, guests);
        Api.BookingView created = bookings.create(userId, room.roomId(), room.hotelId(), checkIn, checkOut, guests, room.basePrice(), price);
        try {
            remotes.hold(room.roomId(), checkIn, checkOut, created.id());
        } catch (RuntimeException ex) {
            bookings.abort(created.id());
            throw ex;
        }
        send(Topics.BOOKING_CREATED, created.id(), new BusEvent(
                BusEvent.BOOKING_CREATED, created.id(), userId, room.hotelId(), price.total().toPlainString(),
                checkIn.toString(), checkOut.toString(), null, null
        ));
        return created;
    }

    public Api.BookingView pay(long userId, long bookingId, String method) {
        try {
            Api.BookingView paid = bookings.pay(userId, bookingId);
            remotes.pay(paid.id(), userId, BigDecimal.valueOf(paid.totalPrice()), method);
            remotes.confirmSold(paid.id());
            send(Topics.BOOKING_PAID, paid.id(), new BusEvent(
                    BusEvent.BOOKING_PAID, paid.id(), userId, paid.hotelId(), BigDecimal.valueOf(paid.totalPrice()).toPlainString(),
                    paid.checkIn(), paid.checkOut(), remotes.activeRooms(paid.hotelId()), null
            ));
            return paid;
        } catch (Failures.HoldExpiredException ex) {
            remotes.release(ex.bookingId());
            throw ex;
        }
    }

    public Api.BookingView cancel(long actorId, boolean staff, long bookingId) {
        Api.BookingView cancelled = bookings.cancel(actorId, staff, bookingId);
        remotes.release(cancelled.id());
        send(Topics.BOOKING_CANCELLED, cancelled.id(), new BusEvent(
                BusEvent.BOOKING_CANCELLED, cancelled.id(), actorId, cancelled.hotelId(), null,
                cancelled.checkIn(), cancelled.checkOut(), null, null
        ));
        return cancelled;
    }

    public void sweep() {
        for (Long id : bookings.expireOverdue()) {
            remotes.release(id);
        }
    }

    private void send(String topic, long key, BusEvent event) {
        KafkaTemplate<String, String> template = kafka.getIfAvailable();
        if (template == null) {
            log.warn("Kafka недоступна, событие {} не отправлено", topic);
            return;
        }
        template.send(topic, Long.toString(key), event.json());
    }
}
