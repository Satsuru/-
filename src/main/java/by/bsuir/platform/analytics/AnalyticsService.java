package by.bsuir.platform.analytics;

import by.bsuir.platform.api.Contracts.AnalyticsPort;
import by.bsuir.platform.api.Contracts.OccupancyRevenueView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Order(6)
public class AnalyticsService implements AnalyticsPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    @Override
    public OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to) {
        int total = repository.totalNights(hotelId, from, to);
        int sold = repository.soldNights(hotelId, from, to);
        double occupancy = total == 0 ? 0 : BigDecimal.valueOf(sold)
                .divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP)
                .doubleValue();
        return new OccupancyRevenueView(
                hotelId,
                occupancy,
                repository.revenue(hotelId, from, to).doubleValue(),
                from.toString(),
                to.toString()
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Analytics: своя схема analytics, фактов продаж {}", repository.countFacts());
    }
}
