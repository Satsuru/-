package by.bsuir.platform.pricing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
class PricingRepository {

    private final JdbcTemplate jdbc;

    PricingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    BigDecimal occupancyMultiplier(long hotelId, BigDecimal ratio) {
        List<BigDecimal> rows = jdbc.query(
                """
                select multiplier from pricing.occupancy_rules
                where hotel_id = ? and min_occupancy <= ? and max_occupancy > ?
                order by min_occupancy
                """,
                (rs, n) -> rs.getBigDecimal("multiplier"),
                hotelId, ratio, ratio
        );
        return rows.isEmpty() ? BigDecimal.ONE : rows.get(0);
    }

    BigDecimal seasonMultiplier(long hotelId, int month) {
        return one(
                "select multiplier from pricing.seasonal_month_rules where hotel_id = ? and month_number = ?",
                hotelId, month
        );
    }

    BigDecimal weekdayMultiplier(long hotelId, int dayOfWeek) {
        return one(
                "select multiplier from pricing.weekday_rules where hotel_id = ? and day_of_week = ?",
                hotelId, dayOfWeek
        );
    }

    BigDecimal demandMultiplier(long hotelId, LocalDate checkIn) {
        List<DemandRule> rules = jdbc.query(
                "select window_days, view_threshold, multiplier from pricing.demand_rules where hotel_id = ?",
                (rs, n) -> new DemandRule(rs.getInt("window_days"), rs.getInt("view_threshold"), rs.getBigDecimal("multiplier")),
                hotelId
        );
        if (rules.isEmpty()) {
            return BigDecimal.ONE;
        }
        DemandRule rule = rules.get(0);
        Integer views = jdbc.queryForObject(
                """
                select coalesce(sum(views), 0) from pricing.hotel_view_stats
                where hotel_id = ? and view_date >= ? and view_date < ?
                """,
                Integer.class,
                hotelId, checkIn.minusDays(rule.windowDays()), checkIn
        );
        int seen = views == null ? 0 : views;
        return seen >= rule.threshold() ? rule.multiplier() : BigDecimal.ONE;
    }

    GuestRule guestRule(long hotelId) {
        List<GuestRule> rows = jdbc.query(
                "select included_guests, extra_multiplier from pricing.guest_rules where hotel_id = ?",
                (rs, n) -> new GuestRule(rs.getInt("included_guests"), rs.getBigDecimal("extra_multiplier")),
                hotelId
        );
        return rows.isEmpty() ? new GuestRule(99, BigDecimal.ZERO) : rows.get(0);
    }

    int countRules() {
        Integer count = jdbc.queryForObject("select count(*) from pricing.occupancy_rules", Integer.class);
        return count == null ? 0 : count;
    }

    private BigDecimal one(String sql, long hotelId, int value) {
        List<BigDecimal> rows = jdbc.query(sql, (rs, n) -> rs.getBigDecimal("multiplier"), hotelId, value);
        return rows.isEmpty() ? BigDecimal.ONE : rows.get(0);
    }

    record DemandRule(int windowDays, int threshold, BigDecimal multiplier) {
    }

    record GuestRule(int includedGuests, BigDecimal extraMultiplier) {
    }
}
