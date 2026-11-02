package by.bsuir.platform.pricing;

import by.bsuir.platform.contracts.PriceMath;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
class PricingRepository {

    private final JdbcTemplate jdbc;

    PricingRepository(@Qualifier("pricingJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    PriceMath.Rules rules(long hotelId) {
        var bands = jdbc.query(
                "select min_occupancy, max_occupancy, multiplier from pricing.occupancy_rules where hotel_id = ? order by min_occupancy",
                (rs, n) -> new PriceMath.Band(rs.getBigDecimal("min_occupancy"), rs.getBigDecimal("max_occupancy"), rs.getBigDecimal("multiplier")),
                hotelId
        );
        Map<Integer, BigDecimal> season = new LinkedHashMap<>();
        jdbc.query("select month_number, multiplier from pricing.seasonal_month_rules where hotel_id = ?",
                rs -> { season.put(rs.getInt("month_number"), rs.getBigDecimal("multiplier")); }, hotelId);
        Map<Integer, BigDecimal> weekday = new LinkedHashMap<>();
        jdbc.query("select day_of_week, multiplier from pricing.weekday_rules where hotel_id = ?",
                rs -> { weekday.put(rs.getInt("day_of_week"), rs.getBigDecimal("multiplier")); }, hotelId);
        var demand = jdbc.query(
                "select window_days, view_threshold, multiplier from pricing.demand_rules where hotel_id = ?",
                (rs, n) -> new int[] {rs.getInt("window_days"), rs.getInt("view_threshold")},
                hotelId
        );
        BigDecimal demandMultiplier = jdbc.query(
                "select multiplier from pricing.demand_rules where hotel_id = ?",
                (rs, n) -> rs.getBigDecimal("multiplier"), hotelId
        ).stream().findFirst().orElse(BigDecimal.ONE);
        Map<LocalDate, Integer> views = new LinkedHashMap<>();
        jdbc.query("select view_date, views from pricing.hotel_view_stats where hotel_id = ?",
                rs -> { views.put(rs.getDate("view_date").toLocalDate(), rs.getInt("views")); }, hotelId);
        var guest = jdbc.query(
                "select included_guests, extra_multiplier from pricing.guest_rules where hotel_id = ?",
                (rs, n) -> new Object[] {rs.getInt("included_guests"), rs.getBigDecimal("extra_multiplier")},
                hotelId
        );
        int included = guest.isEmpty() ? 99 : (Integer) guest.get(0)[0];
        BigDecimal extra = guest.isEmpty() ? BigDecimal.ZERO : (BigDecimal) guest.get(0)[1];
        int window = demand.isEmpty() ? 7 : demand.get(0)[0];
        int threshold = demand.isEmpty() ? Integer.MAX_VALUE : demand.get(0)[1];
        return new PriceMath.Rules(bands, season, weekday, window, threshold, demandMultiplier, views, included, extra);
    }

    int countRules() {
        Integer count = jdbc.queryForObject("select count(*) from pricing.occupancy_rules", Integer.class);
        return count == null ? 0 : count;
    }
}
