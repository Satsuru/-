package by.bsuir.platform.booking;

import by.bsuir.platform.api.Contracts.BookingPort;
import by.bsuir.platform.api.Contracts.BookingView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Order(4)
public class BookingService implements BookingPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BookingView> listMine(long userId) {
        return repository.listMine(userId);
    }

    @Override
    public Optional<BookingView> get(long bookingId, long userId) {
        return repository.get(bookingId, userId);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Booking: своя схема booking, броней {}", repository.countBookings());
    }
}
