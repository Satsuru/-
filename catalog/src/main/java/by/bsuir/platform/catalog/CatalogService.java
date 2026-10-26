package by.bsuir.platform.catalog;

import by.bsuir.platform.contracts.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public Optional<Api.HotelView> hotel(long hotelId) {
        return repository.hotel(hotelId);
    }

    public List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
        return repository.search(city, checkIn, checkOut, guests, minStars);
    }

    public Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return repository.getRoom(roomId, checkIn, checkOut);
    }

    public boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return repository.isFree(roomId, checkIn, checkOut);
    }

    public void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
        repository.hold(roomId, checkIn, checkOut, bookingId);
    }

    public void release(long bookingId) {
        repository.release(bookingId);
    }

    public void confirmSold(long bookingId) {
        repository.confirmSold(bookingId);
    }

    public int heldNights(long bookingId) {
        return repository.heldNights(bookingId);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Catalog: своя схема catalog, отелей {}", repository.countHotels());
    }
}
