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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BookingService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    public List<Api.BookingView> listMine(long userId, String status, Api.BookingSort sort) {
        List<Api.BookingView> rows = new ArrayList<>(repository.listMine(userId, blank(status)));
        if (sort == Api.BookingSort.DATE_DESC) {
            Collections.reverse(rows);
        }
        return rows;
    }

    public Api.BookingView get(long bookingId, long userId) {
        return repository.get(bookingId, userId).orElse(null);
    }

    public Api.BookingStats stats(long userId) {
        return repository.stats(userId);
    }

    public Api.BookingView create(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut,
                                  int guests, BigDecimal basePrice, Api.PriceQuote quote) {
        Dates.requireStay(checkIn, checkOut, guests);
        long id = repository.insert(userId, roomId, hotelId, checkIn, checkOut, guests, quote, basePrice,
                LocalDateTime.now().plusHours(48));
        return repository.find(id).orElseThrow(() -> new Failures.NotFoundException("Бронь не найдена"));
    }

    public Api.BookingView pay(long userId, long bookingId) {
        String status = require(bookingId, userId);
        if ("CREATED".equals(status) && repository.holdExpired(bookingId, LocalDateTime.now())) {
            repository.setStatus(bookingId, "CANCELLED");
            throw new Failures.HoldExpiredException(bookingId);
        }
        BookingRules.checkPay(status);
        repository.setStatus(bookingId, "PAID");
        return repository.get(bookingId, userId).orElseThrow(() -> new Failures.NotFoundException("Бронь не найдена"));
    }

    public Api.BookingView cancel(long actorId, boolean staff, long bookingId) {
        Api.BookingView row = staff
                ? repository.find(bookingId).orElse(null)
                : repository.get(bookingId, actorId).orElse(null);
        if (row == null) {
            throw new Failures.NotFoundException("Бронь не найдена");
        }
        BookingRules.checkCancel(row.status());
        repository.setStatus(bookingId, "CANCELLED");
        return repository.find(bookingId).orElseThrow(() -> new Failures.NotFoundException("Бронь не найдена"));
    }

    public void abort(long bookingId) {
        repository.cancelIfCreated(bookingId);
    }

    public List<Long> expireOverdue() {
        return repository.expireOverdue(LocalDateTime.now());
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
