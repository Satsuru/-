package by.bsuir.platform.pricing;

import by.bsuir.platform.api.Contracts.PriceQuote;
import by.bsuir.platform.api.Contracts.PricingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Order(3)
public class PricingService implements PricingPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);

    private final PricingRepository repository;

    public PricingService(PricingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PriceQuote calculate(
            long hotelId,
            BigDecimal basePrice,
            BigDecimal occupancyRatio,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests
    ) {
        BigDecimal occupancy = scale(repository.occupancyMultiplier(hotelId, occupancyRatio));
        BigDecimal demand = scale(repository.demandMultiplier(hotelId, checkIn));
        BigDecimal guest = scale(guestMultiplier(hotelId, guests));
        BigDecimal season = BigDecimal.ONE;
        BigDecimal weekday = BigDecimal.ONE;
        BigDecimal total = BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            season = scale(repository.seasonMultiplier(hotelId, day.getMonthValue()));
            weekday = scale(repository.weekdayMultiplier(hotelId, day.getDayOfWeek().getValue()));
            BigDecimal multiplier = occupancy.multiply(season).multiply(weekday).multiply(demand).multiply(guest);
            total = total.add(basePrice.multiply(multiplier));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal nightly = nights == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        String explanation = "Ночей: " + nights
                + ". Загрузка " + occupancy
                + ", сезон " + season
                + ", день недели " + weekday
                + ", спрос " + demand
                + ", гости " + guest
                + ". Итог " + total + ".";
        return new PriceQuote(nightly, total, occupancy, season, weekday, demand, guest, explanation);
    }

    private BigDecimal guestMultiplier(long hotelId, int guests) {
        PricingRepository.GuestRule rule = repository.guestRule(hotelId);
        int extra = Math.max(0, guests - rule.includedGuests());
        return BigDecimal.ONE.add(rule.extraMultiplier().multiply(BigDecimal.valueOf(extra)));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Pricing: своя схема pricing, правил загрузки {}", repository.countRules());
    }
}
