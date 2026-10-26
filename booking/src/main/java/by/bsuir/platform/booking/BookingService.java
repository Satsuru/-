package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.BookingRules;
import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.Failures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    public List<Api.BookingView> listMine(long userId, String status) {
        return repository.listMine(userId, blank(status));
    }

    public Api.BookingView get(long bookingId, long userId) {
        return repository.get(bookingId, userId).orElse(null);
    }

    public long create(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut,
                       int guests, BigDecimal basePrice, Api.PriceQuote quote) {
        Dates.requireStay(checkIn, checkOut, guests);
        return repository.insert(userId, roomId, hotelId, checkIn, checkOut, guests, quote, basePrice);
    }

    public void pay(long userId, long bookingId) {
        String status = require(bookingId, userId);
        BookingRules.checkPay(status);
        repository.setStatus(bookingId, "PAID");
    }

    public void cancel(long userId, long bookingId) {
        String status = require(bookingId, userId);
        BookingRules.checkCancel(status);
        repository.setStatus(bookingId, "CANCELLED");
    }

    private String require(long bookingId, long userId) {
        String status = repository.status(bookingId, userId);
        if (status == null) {
            throw new Failures.NotFoundException("Бронь не найдена");
        }
        return status;
    }

    private String blank(String status) {
        return status == null || status.isBlank() ? null : status;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Booking: своя схема booking, броней {}", repository.countBookings());
    }
}
