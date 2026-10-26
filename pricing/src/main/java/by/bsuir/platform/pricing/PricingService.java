package by.bsuir.platform.pricing;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.PriceMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PricingService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);
    private final PricingRepository repository;

    public PricingService(PricingRepository repository) {
        this.repository = repository;
    }

    public Api.PriceQuote calculate(long hotelId, BigDecimal basePrice, BigDecimal occupancyRatio,
                                    LocalDate checkIn, LocalDate checkOut, int guests) {
        return PriceMath.quote(basePrice, occupancyRatio, checkIn, checkOut, guests, repository.rules(hotelId));
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Pricing: своя схема pricing, правил загрузки {}", repository.countRules());
    }
}
