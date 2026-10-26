package by.bsuir.platform.booking;

import by.bsuir.platform.contracts.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class BookingRepository {

    private final JdbcTemplate jdbc;

    BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Api.BookingView> listMine(long userId, String status) {
        return jdbc.query(
                """
                select b.id, b.room_id, b.hotel_id, b.check_in, b.check_out, b.guests, b.status,
                       b.total_price, b.hold_until,
                       s.base_price, s.occupancy_multiplier, s.season_multiplier, s.weekday_multiplier,
                       s.demand_multiplier, s.guest_multiplier, s.calculated_price
                from booking.bookings b
                left join booking.price_snapshots s on s.booking_id = b.id
                where b.user_id = ? and (? is null or b.status = ?)
                order by b.check_in
                """,
                (rs, n) -> map(rs),
                userId, status, status
        );
    }

    Optional<Api.BookingView> get(long bookingId, long userId) {
        return listMine(userId, null).stream().filter(row -> row.id().equals(bookingId)).findFirst();
    }

    String status(long bookingId, long userId) {
        List<String> rows = jdbc.query(
                "select status from booking.bookings where id = ? and user_id = ?",
                (rs, n) -> rs.getString("status"),
                bookingId, userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    long insert(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, Api.PriceQuote quote, BigDecimal basePrice) {
        Long id = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from booking.bookings", Long.class);
        jdbc.update("""
                insert into booking.bookings
                (id, user_id, room_id, hotel_id, check_in, check_out, guests, status, total_price, hold_until)
                values (?, ?, ?, ?, ?, ?, ?, 'CREATED', ?, ?)
                """, id, userId, roomId, hotelId, checkIn, checkOut, guests, quote.total(),
                java.sql.Timestamp.valueOf(checkIn.minusDays(1).atTime(18, 0)));
        Long snapshotId = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from booking.price_snapshots", Long.class);
        jdbc.update("""
                insert into booking.price_snapshots
                (id, booking_id, base_price, occupancy_multiplier, season_multiplier, weekday_multiplier,
                 demand_multiplier, guest_multiplier, calculated_price)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, snapshotId, id, basePrice, quote.occupancy(), quote.season(), quote.weekday(),
                quote.demand(), quote.guest(), quote.total());
        return id;
    }

    void setStatus(long bookingId, String status) {
        jdbc.update("update booking.bookings set status = ?, hold_until = null where id = ?", status, bookingId);
    }

    int countBookings() {
        Integer count = jdbc.queryForObject("select count(*) from booking.bookings", Integer.class);
        return count == null ? 0 : count;
    }

    private Api.BookingView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Api.SnapshotView snapshot = rs.getBigDecimal("base_price") == null ? null : new Api.SnapshotView(
                rs.getDouble("base_price"),
                rs.getDouble("occupancy_multiplier"),
                rs.getDouble("season_multiplier"),
                rs.getDouble("weekday_multiplier"),
                rs.getDouble("demand_multiplier"),
                rs.getDouble("guest_multiplier"),
                rs.getDouble("calculated_price")
        );
        var hold = rs.getTimestamp("hold_until");
        return new Api.BookingView(
                rs.getLong("id"), rs.getLong("room_id"), rs.getLong("hotel_id"),
                rs.getDate("check_in").toLocalDate().toString(),
                rs.getDate("check_out").toLocalDate().toString(),
                rs.getInt("guests"), rs.getString("status"), rs.getDouble("total_price"),
                hold == null ? null : hold.toInstant().toString(), snapshot
        );
    }
}
