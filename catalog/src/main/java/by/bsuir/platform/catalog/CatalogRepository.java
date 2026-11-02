package by.bsuir.platform.catalog;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Failures;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
class CatalogRepository {

    private final JdbcTemplate jdbc;

    CatalogRepository(@Qualifier("catalogJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Api.HotelView> hotel(long hotelId) {
        return jdbc.query(
                "select id, name, city, address, star_rating from catalog.hotels where id = ?",
                (rs, n) -> new Api.HotelView(rs.getLong("id"), rs.getString("name"), rs.getString("city"), rs.getString("address"), rs.getDouble("star_rating")),
                hotelId
        ).stream().findFirst();
    }

    List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
        return jdbc.query(
                """
                select r.id as room_id, h.id as hotel_id, h.name, h.city, h.address, h.star_rating,
                       r.room_type, r.capacity, r.base_price
                from catalog.rooms r
                join catalog.hotels h on h.id = r.hotel_id
                where lower(h.city) = lower(?)
                  and r.capacity >= ?
                  and r.active = true
                  and (? is null or h.star_rating >= ?)
                  and not exists (
                      select 1 from catalog.room_nights n
                      where n.room_id = r.id
                        and n.stay_date >= ? and n.stay_date < ?
                        and n.state in ('HELD', 'SOLD')
                  )
                order by r.base_price
                """,
                (rs, n) -> card(rs, checkIn, checkOut),
                city, guests, minStars, minStars, checkIn, checkOut
        );
    }

    Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.query(
                """
                select r.id as room_id, h.id as hotel_id, h.name, h.city, h.address, h.star_rating,
                       r.room_type, r.capacity, r.base_price
                from catalog.rooms r
                join catalog.hotels h on h.id = r.hotel_id
                where r.id = ? and r.active = true
                """,
                (rs, n) -> card(rs, checkIn, checkOut),
                roomId
        ).stream().findFirst();
    }

    boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
        Integer blocked = jdbc.queryForObject(
                """
                select count(*) from catalog.room_nights
                where room_id = ? and stay_date >= ? and stay_date < ? and state in ('HELD', 'SOLD')
                """,
                Integer.class, roomId, checkIn, checkOut
        );
        return blocked == null || blocked == 0;
    }

    void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
        if (!isFree(roomId, checkIn, checkOut)) {
            throw new Failures.BadRequestException("Номер занят на эти даты");
        }
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            jdbc.update("insert into catalog.room_nights (room_id, stay_date, state, booking_id) values (?, ?, 'HELD', ?)",
                    roomId, day, bookingId);
        }
    }

    void release(long bookingId) {
        jdbc.update("delete from catalog.room_nights where booking_id = ? and state = 'HELD'", bookingId);
    }

    void confirmSold(long bookingId) {
        jdbc.update("update catalog.room_nights set state = 'SOLD' where booking_id = ? and state = 'HELD'", bookingId);
    }

    int heldNights(long bookingId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from catalog.room_nights where booking_id = ? and state = 'HELD'",
                Integer.class, bookingId
        );
        return count == null ? 0 : count;
    }

    int activeRooms(long hotelId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from catalog.rooms where hotel_id = ? and active = true",
                Integer.class, hotelId
        );
        return count == null ? 0 : count;
    }

    int countHotels() {
        Integer count = jdbc.queryForObject("select count(*) from catalog.hotels", Integer.class);
        return count == null ? 0 : count;
    }

    private Api.RoomCard card(java.sql.ResultSet rs, LocalDate checkIn, LocalDate checkOut) throws java.sql.SQLException {
        long hotelId = rs.getLong("hotel_id");
        return new Api.RoomCard(
                rs.getLong("room_id"),
                hotelId,
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getDouble("star_rating"),
                rs.getString("room_type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("base_price"),
                occupancy(hotelId, checkIn, checkOut)
        );
    }

    private BigDecimal occupancy(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Integer rooms = jdbc.queryForObject(
                "select count(*) from catalog.rooms where hotel_id = ? and active = true",
                Integer.class, hotelId
        );
        long slots = (rooms == null ? 0 : rooms) * nights;
        if (slots == 0) {
            return BigDecimal.ZERO;
        }
        Integer blocked = jdbc.queryForObject(
                """
                select count(*) from catalog.room_nights n
                join catalog.rooms r on r.id = n.room_id
                where r.hotel_id = ? and n.stay_date >= ? and n.stay_date < ?
                  and n.state in ('HELD', 'SOLD')
                """,
                Integer.class, hotelId, checkIn, checkOut
        );
        return BigDecimal.valueOf(blocked == null ? 0 : blocked).divide(BigDecimal.valueOf(slots), 3, RoundingMode.HALF_UP);
    }
}
