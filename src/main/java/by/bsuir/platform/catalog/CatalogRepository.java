package by.bsuir.platform.catalog;

import by.bsuir.platform.api.Contracts.HotelView;
import by.bsuir.platform.api.Contracts.RoomCard;
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

    CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<HotelView> hotel(long hotelId) {
        List<HotelView> rows = jdbc.query(
                "select id, name, city, address, star_rating from catalog.hotels where id = ?",
                (rs, n) -> new HotelView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("city"),
                        rs.getString("address"),
                        rs.getDouble("star_rating")
                ),
                hotelId
        );
        return rows.stream().findFirst();
    }

    List<RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests) {
        List<RoomCard> rooms = jdbc.query(
                """
                select r.id as room_id, h.id as hotel_id, h.name, h.city, h.address, h.star_rating,
                       r.room_type, r.capacity, r.base_price
                from catalog.rooms r
                join catalog.hotels h on h.id = r.hotel_id
                where lower(h.city) = lower(?)
                  and r.capacity >= ?
                  and r.active = true
                  and not exists (
                      select 1 from catalog.room_nights n
                      where n.room_id = r.id
                        and n.stay_date >= ? and n.stay_date < ?
                        and n.state in ('HELD', 'SOLD')
                  )
                order by r.base_price
                """,
                (rs, n) -> card(rs),
                city, guests, checkIn, checkOut
        );
        return rooms.stream()
                .map(room -> withOccupancy(room, checkIn, checkOut))
                .toList();
    }

    Optional<RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<RoomCard> rows = jdbc.query(
                """
                select r.id as room_id, h.id as hotel_id, h.name, h.city, h.address, h.star_rating,
                       r.room_type, r.capacity, r.base_price
                from catalog.rooms r
                join catalog.hotels h on h.id = r.hotel_id
                where r.id = ? and r.active = true
                """,
                (rs, n) -> card(rs),
                roomId
        );
        return rows.stream().findFirst().map(room -> withOccupancy(room, checkIn, checkOut));
    }

    boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
        Integer blocked = jdbc.queryForObject(
                """
                select count(*) from catalog.room_nights
                where room_id = ? and stay_date >= ? and stay_date < ? and state in ('HELD', 'SOLD')
                """,
                Integer.class,
                roomId, checkIn, checkOut
        );
        return blocked == null || blocked == 0;
    }

    int countHotels() {
        Integer count = jdbc.queryForObject("select count(*) from catalog.hotels", Integer.class);
        return count == null ? 0 : count;
    }

    private RoomCard withOccupancy(RoomCard room, LocalDate checkIn, LocalDate checkOut) {
        return new RoomCard(
                room.roomId(), room.hotelId(), room.hotelName(), room.city(), room.address(),
                room.starRating(), room.roomType(), room.capacity(), room.basePrice(),
                occupancy(room.hotelId(), checkIn, checkOut)
        );
    }

    private BigDecimal occupancy(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        Integer rooms = jdbc.queryForObject(
                "select count(*) from catalog.rooms where hotel_id = ? and active = true",
                Integer.class,
                hotelId
        );
        int roomCount = rooms == null ? 0 : rooms;
        long slots = roomCount * nights;
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
                Integer.class,
                hotelId, checkIn, checkOut
        );
        int taken = blocked == null ? 0 : blocked;
        return BigDecimal.valueOf(taken).divide(BigDecimal.valueOf(slots), 3, RoundingMode.HALF_UP);
    }

    private RoomCard card(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RoomCard(
                rs.getLong("room_id"),
                rs.getLong("hotel_id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getDouble("star_rating"),
                rs.getString("room_type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("base_price"),
                BigDecimal.ZERO
        );
    }
}
