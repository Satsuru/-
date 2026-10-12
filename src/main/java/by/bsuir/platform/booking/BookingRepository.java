package by.bsuir.platform.booking;

import by.bsuir.platform.api.Contracts.BookingView;
import by.bsuir.platform.api.Contracts.SnapshotView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class BookingRepository {

    private final JdbcTemplate jdbc;

    BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<BookingView> listMine(long userId) {
        return jdbc.query(
                """
                select b.id, b.room_id, b.hotel_id, b.check_in, b.check_out, b.guests, b.status,
                       b.total_price, b.hold_until,
                       s.base_price, s.occupancy_multiplier, s.season_multiplier, s.weekday_multiplier,
                       s.demand_multiplier, s.guest_multiplier, s.calculated_price
                from booking.bookings b
                left join booking.price_snapshots s on s.booking_id = b.id
                where b.user_id = ?
                order by b.id
                """,
                (rs, n) -> map(rs),
                userId
        );
    }

    Optional<BookingView> get(long bookingId, long userId) {
        return listMine(userId).stream().filter(row -> row.id().equals(bookingId)).findFirst();
    }

    int countBookings() {
        Integer count = jdbc.queryForObject("select count(*) from booking.bookings", Integer.class);
        return count == null ? 0 : count;
    }

    private BookingView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        SnapshotView snapshot = rs.getBigDecimal("base_price") == null ? null : new SnapshotView(
                rs.getDouble("base_price"),
                rs.getDouble("occupancy_multiplier"),
                rs.getDouble("season_multiplier"),
                rs.getDouble("weekday_multiplier"),
                rs.getDouble("demand_multiplier"),
                rs.getDouble("guest_multiplier"),
                rs.getDouble("calculated_price")
        );
        var hold = rs.getTimestamp("hold_until");
        return new BookingView(
                rs.getLong("id"),
                rs.getLong("room_id"),
                rs.getLong("hotel_id"),
                rs.getDate("check_in").toLocalDate().toString(),
                rs.getDate("check_out").toLocalDate().toString(),
                rs.getInt("guests"),
                rs.getString("status"),
                rs.getDouble("total_price"),
                hold == null ? null : hold.toInstant().toString(),
                snapshot
        );
    }
}
