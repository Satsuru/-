package by.bsuir.platform.analytics;

import by.bsuir.platform.contracts.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private final JdbcTemplate jdbc;

    public AnalyticsService(@Qualifier("analyticsJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Api.OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to) {
        BigDecimal revenue = jdbc.queryForObject("""
                select coalesce(sum(amount), 0) from analytics.sales_facts
                where hotel_id = ? and status = 'PAID' and occurred_on >= ? and occurred_on <= ?
                """, BigDecimal.class, hotelId, from, to);
        Integer sold = jdbc.queryForObject("""
                select coalesce(sum(sold_nights), 0) from analytics.occupancy_facts
                where hotel_id = ? and stay_date >= ? and stay_date <= ?
                """, Integer.class, hotelId, from, to);
        Integer total = jdbc.queryForObject("""
                select coalesce(sum(total_nights), 0) from analytics.occupancy_facts
                where hotel_id = ? and stay_date >= ? and stay_date <= ?
                """, Integer.class, hotelId, from, to);
        int soldNights = sold == null ? 0 : sold;
        int totalNights = total == null ? 0 : total;
        double ratio = totalNights == 0 ? 0 : BigDecimal.valueOf(soldNights)
                .divide(BigDecimal.valueOf(totalNights), 3, RoundingMode.HALF_UP).doubleValue();
        return new Api.OccupancyRevenueView(hotelId, ratio, revenue == null ? 0 : revenue.doubleValue(), from.toString(), to.toString());
    }

    public void addSale(long hotelId, String status, BigDecimal amount, LocalDate day) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from analytics.sales_facts", Long.class);
        jdbc.update("insert into analytics.sales_facts (id, hotel_id, status, amount, occurred_on) values (?, ?, ?, ?, ?)",
                id, hotelId, status, amount, day);
    }

    public void recordSoldNights(long hotelId, LocalDate checkIn, LocalDate checkOut, int totalRooms) {
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            List<Long> ids = jdbc.query(
                    "select id from analytics.occupancy_facts where hotel_id = ? and stay_date = ?",
                    (rs, n) -> rs.getLong("id"),
                    hotelId, day
            );
            if (ids.isEmpty()) {
                Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from analytics.occupancy_facts", Long.class);
                jdbc.update("""
                        insert into analytics.occupancy_facts (id, hotel_id, stay_date, sold_nights, total_nights)
                        values (?, ?, ?, 1, ?)
                        """, id, hotelId, day, totalRooms);
            } else {
                jdbc.update(
                        "update analytics.occupancy_facts set sold_nights = sold_nights + 1, total_nights = ? where id = ?",
                        totalRooms, ids.get(0)
                );
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("select count(*) from analytics.sales_facts", Integer.class);
        log.info("Analytics: своя схема analytics, фактов продаж {}", count);
    }
}
