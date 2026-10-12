package by.bsuir.platform.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    AnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    BigDecimal revenue(long hotelId, LocalDate from, LocalDate to) {
        BigDecimal sum = jdbc.queryForObject(
                """
                select coalesce(sum(amount), 0) from analytics.sales_facts
                where hotel_id = ? and status = 'PAID' and occurred_on >= ? and occurred_on <= ?
                """,
                BigDecimal.class,
                hotelId, from, to
        );
        return sum == null ? BigDecimal.ZERO : sum;
    }

    int soldNights(long hotelId, LocalDate from, LocalDate to) {
        return sum("sold_nights", hotelId, from, to);
    }

    int totalNights(long hotelId, LocalDate from, LocalDate to) {
        return sum("total_nights", hotelId, from, to);
    }

    int countFacts() {
        Integer count = jdbc.queryForObject("select count(*) from analytics.sales_facts", Integer.class);
        return count == null ? 0 : count;
    }

    private int sum(String column, long hotelId, LocalDate from, LocalDate to) {
        Integer value = jdbc.queryForObject(
                "select coalesce(sum(" + column + "), 0) from analytics.occupancy_facts where hotel_id = ? and stay_date >= ? and stay_date <= ?",
                Integer.class,
                hotelId, from, to
        );
        return value == null ? 0 : value;
    }
}
