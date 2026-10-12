package by.bsuir.platform.catalog;

import by.bsuir.platform.api.Contracts.CatalogPort;
import by.bsuir.platform.api.Contracts.HotelView;
import by.bsuir.platform.api.Contracts.RoomCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Order(2)
public class CatalogService implements CatalogPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<HotelView> hotel(long hotelId) {
        return repository.hotel(hotelId);
    }

    @Override
    public List<RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests) {
        return repository.search(city, checkIn, checkOut, guests);
    }

    @Override
    public Optional<RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return repository.getRoom(roomId, checkIn, checkOut);
    }

    @Override
    public boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return repository.isFree(roomId, checkIn, checkOut);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Catalog: своя схема catalog, отелей {}", repository.countHotels());
    }
}
