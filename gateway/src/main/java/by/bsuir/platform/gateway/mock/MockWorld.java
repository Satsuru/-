package by.bsuir.platform.gateway.mock;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.BookingRules;
import by.bsuir.platform.contracts.Dates;
import by.bsuir.platform.contracts.Failures;
import by.bsuir.platform.contracts.PriceMath;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Память заглушек шлюза. Это не база сервисов и не их классы.
 * Цифры совпадают с data.sql модулей identity, catalog, pricing, booking,
 * payment, assistant, notification и analytics.
 */
public class MockWorld {

    final List<UserRow> users = new ArrayList<>();
    final List<String> roles = new ArrayList<>();
    final Map<String, Long> tokens = new LinkedHashMap<>();
    final List<HotelRow> hotels = new ArrayList<>();
    final List<RoomRow> rooms = new ArrayList<>();
    final List<NightRow> nights = new ArrayList<>();
    final PriceMath.Rules rules;
    final List<BookingRow> bookings = new ArrayList<>();
    final List<PaymentRow> payments = new ArrayList<>();
    final List<DialogRow> dialogs = new ArrayList<>();
    final List<MessageRow> messages = new ArrayList<>();
    final List<NoteRow> notes = new ArrayList<>();
    final List<SaleRow> sales = new ArrayList<>();
    final List<OccRow> occupancy = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    public MockWorld() {
        users.add(new UserRow(1, "guest@hotel.local", "guest", "Анна Гость", "+375291112233", true));
        users.add(new UserRow(2, "worker@hotel.local", "worker", "Иван Сотрудник", null, true));
        roles.add("1:GUEST");
        roles.add("2:WORKER");
        hotels.add(new HotelRow(1, "Неман", "Минск", "ул. Кирова, 1", 4.0));
        rooms.add(new RoomRow(1, 1, "Стандарт", 2, new BigDecimal("100.00"), true));
        rooms.add(new RoomRow(2, 1, "Люкс", 3, new BigDecimal("180.00"), true));
        nights.add(new NightRow(1, LocalDate.parse("2026-06-10"), "HELD", 1L));
        nights.add(new NightRow(1, LocalDate.parse("2026-06-11"), "HELD", 1L));
        nights.add(new NightRow(2, LocalDate.parse("2026-06-01"), "SOLD", 2L));
        rules = demoRules();
        bookings.add(booking1());
        bookings.add(booking2());
        payments.add(new PaymentRow(1, 2, 1, new BigDecimal("308.88"), "CARD", "COMPLETED"));
        dialogs.add(new DialogRow(1, 1, "OPEN"));
        messages.add(new MessageRow(1, 1, "GUEST", "Сколько стоит стандарт в июне?"));
        messages.add(new MessageRow(2, 1, "ASSISTANT", "Цена считается по правилам отеля на даты заезда."));
        notes.add(new NoteRow(1, 1, "BOOKING_CREATED", "Бронь 1 создана, номер удерживается до оплаты", "NEW", "2026-06-01T07:00:00Z"));
        notes.add(new NoteRow(2, 1, "PAYMENT_COMPLETED", "Бронь 2 оплачена", "READ", "2026-06-01T09:00:00Z"));
        sales.add(new SaleRow(1, 1, "PAID", new BigDecimal("308.88"), LocalDate.parse("2026-06-01")));
        sales.add(new SaleRow(2, 1, "CREATED", new BigDecimal("312.00"), LocalDate.parse("2026-06-10")));
        occupancy.add(new OccRow(1, 1, LocalDate.parse("2026-06-01"), 1, 2));
    }

    private static PriceMath.Rules demoRules() {
        Map<Integer, BigDecimal> season = new LinkedHashMap<>();
        season.put(1, new BigDecimal("0.900"));
        season.put(6, new BigDecimal("1.200"));
        Map<Integer, BigDecimal> weekday = new LinkedHashMap<>();
        for (int day = 1; day <= 5; day++) {
            weekday.put(day, new BigDecimal("1.000"));
        }
        weekday.put(6, new BigDecimal("1.150"));
        weekday.put(7, new BigDecimal("1.150"));
        Map<LocalDate, Integer> views = new LinkedHashMap<>();
        views.put(LocalDate.parse("2026-05-28"), 15);
        views.put(LocalDate.parse("2026-05-30"), 10);
        return new PriceMath.Rules(
                List.of(
                        new PriceMath.Band(new BigDecimal("0.000"), new BigDecimal("0.500"), new BigDecimal("1.000")),
                        new PriceMath.Band(new BigDecimal("0.500"), new BigDecimal("1.010"), new BigDecimal("1.300"))
                ),
                season,
                weekday,
                7,
                20,
                new BigDecimal("1.100"),
                views,
                2,
                new BigDecimal("0.100")
        );
    }

    private static BookingRow booking1() {
        Api.SnapshotView snapshot = new Api.SnapshotView(100.0, 1.3, 1.2, 1.0, 1.0, 1.0, 312.0);
        return new BookingRow(1, 1, 1, 1, LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-12"),
                2, "CREATED", new BigDecimal("312.00"), "2026-06-09T15:00:00Z", snapshot);
    }

    private static BookingRow booking2() {
        Api.SnapshotView snapshot = new Api.SnapshotView(180.0, 1.3, 1.2, 1.0, 1.1, 1.0, 308.88);
        return new BookingRow(2, 1, 2, 1, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-02"),
                2, "PAID", new BigDecimal("308.88"), null, snapshot);
    }

    Api.UserView userView(UserRow user) {
        List<String> codes = roles.stream()
                .filter(row -> row.startsWith(user.id + ":"))
                .map(row -> row.substring(row.indexOf(':') + 1))
                .toList();
        return new Api.UserView(user.id, user.email, user.fullName, user.phone, codes);
    }

    Optional<UserRow> user(long id) {
        return users.stream().filter(row -> row.id == id && row.active).findFirst();
    }

    Api.AuthPayload issue(UserRow user) {
        String token = "user-" + user.id;
        tokens.put(token, user.id);
        return new Api.AuthPayload(token, userView(user));
    }

    long nextId() {
        return ids.incrementAndGet();
    }

    Optional<HotelRow> hotel(long id) {
        return hotels.stream().filter(row -> row.id == id).findFirst();
    }

    Optional<RoomRow> room(long id) {
        return rooms.stream().filter(row -> row.id == id && row.active).findFirst();
    }

    BigDecimal occupancy(long hotelId, LocalDate checkIn, LocalDate checkOut) {
        long nightsCount = ChronoUnit.DAYS.between(checkIn, checkOut);
        long roomCount = rooms.stream().filter(row -> row.hotelId == hotelId && row.active).count();
        long slots = roomCount * nightsCount;
        if (slots == 0) {
            return BigDecimal.ZERO;
        }
        long blocked = nights.stream()
                .filter(night -> room(night.roomId).map(row -> row.hotelId).orElse(0L) == hotelId)
                .filter(night -> !night.date.isBefore(checkIn) && night.date.isBefore(checkOut))
                .filter(night -> "HELD".equals(night.state) || "SOLD".equals(night.state))
                .count();
        return BigDecimal.valueOf(blocked).divide(BigDecimal.valueOf(slots), 3, RoundingMode.HALF_UP);
    }

    boolean free(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return nights.stream().noneMatch(night -> night.roomId == roomId
                && !night.date.isBefore(checkIn) && night.date.isBefore(checkOut)
                && ("HELD".equals(night.state) || "SOLD".equals(night.state)));
    }

    Api.RoomCard card(RoomRow room, LocalDate checkIn, LocalDate checkOut) {
        HotelRow hotel = hotel(room.hotelId).orElseThrow();
        return new Api.RoomCard(room.id, hotel.id, hotel.name, hotel.city, hotel.address, hotel.stars,
                room.type, room.capacity, room.basePrice, occupancy(hotel.id, checkIn, checkOut));
    }

    List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
        return rooms.stream()
                .filter(RoomRow::active)
                .filter(room -> room.capacity >= guests)
                .filter(room -> free(room.id, checkIn, checkOut))
                .map(room -> card(room, checkIn, checkOut))
                .filter(card -> card.city().equalsIgnoreCase(city))
                .filter(card -> minStars == null || card.starRating() >= minStars)
                .sorted(Comparator.comparing(Api.RoomCard::basePrice))
                .toList();
    }

    void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
        if (!free(roomId, checkIn, checkOut)) {
            throw new Failures.BadRequestException("Номер занят на эти даты");
        }
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            nights.add(new NightRow(roomId, day, "HELD", bookingId));
        }
    }

    void release(long bookingId) {
        nights.removeIf(night -> bookingId == night.bookingId && "HELD".equals(night.state));
    }

    void confirm(long bookingId) {
        for (int i = 0; i < nights.size(); i++) {
            NightRow night = nights.get(i);
            if (night.bookingId != null && night.bookingId == bookingId && "HELD".equals(night.state)) {
                nights.set(i, new NightRow(night.roomId, night.date, "SOLD", bookingId));
            }
        }
    }

    Api.BookingView view(BookingRow row) {
        return new Api.BookingView(row.id, row.roomId, row.hotelId, row.checkIn.toString(), row.checkOut.toString(),
                row.guests, row.status, row.total.doubleValue(), row.holdUntil, row.snapshot);
    }

    Optional<BookingRow> booking(long id, long userId) {
        return bookings.stream().filter(row -> row.id == id && row.userId == userId).findFirst();
    }

    Api.BookingView createBooking(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, Api.PriceQuote quote) {
        Dates.requireStay(checkIn, checkOut, guests);
        long id = nextId();
        Api.SnapshotView snapshot = new Api.SnapshotView(
                money(rooms.stream().filter(room -> room.id == roomId).findFirst().orElseThrow().basePrice),
                quote.occupancy().doubleValue(),
                quote.season().doubleValue(),
                quote.weekday().doubleValue(),
                quote.demand().doubleValue(),
                quote.guest().doubleValue(),
                quote.total().doubleValue()
        );
        String holdUntil = checkIn.minusDays(1).atTime(18, 0).toString();
        BookingRow row = new BookingRow(id, userId, roomId, hotelId, checkIn, checkOut, guests, "CREATED", quote.total(), holdUntil, snapshot);
        bookings.add(row);
        return view(row);
    }

    Api.BookingView pay(long userId, long bookingId) {
        BookingRow row = booking(bookingId, userId).orElseThrow(() -> new Failures.NotFoundException("Бронь не найдена"));
        BookingRules.checkPay(row.status);
        row.status = "PAID";
        row.holdUntil = null;
        return view(row);
    }

    Api.BookingView cancel(long userId, long bookingId) {
        BookingRow row = booking(bookingId, userId).orElseThrow(() -> new Failures.NotFoundException("Бронь не найдена"));
        BookingRules.checkCancel(row.status);
        row.status = "CANCELLED";
        row.holdUntil = null;
        return view(row);
    }

    Api.BookingStats stats(long userId) {
        List<BookingRow> mine = bookings.stream().filter(row -> row.userId == userId).toList();
        int created = (int) mine.stream().filter(row -> "CREATED".equals(row.status)).count();
        int paid = (int) mine.stream().filter(row -> "PAID".equals(row.status)).count();
        int cancelled = (int) mine.stream().filter(row -> "CANCELLED".equals(row.status)).count();
        BigDecimal sum = mine.stream()
                .filter(row -> "PAID".equals(row.status))
                .map(row -> row.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Api.BookingStats(mine.size(), created, paid, cancelled, sum.doubleValue());
    }

    void addNote(long userId, String kind, String body) {
        notes.add(new NoteRow(nextId(), userId, kind, body, "NEW", Instant.now().toString()));
    }

    void addSale(long hotelId, String status, BigDecimal amount, LocalDate day) {
        sales.add(new SaleRow(nextId(), hotelId, status, amount, day));
    }

    static double money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static final class UserRow {
        final long id;
        final String email;
        final String password;
        final String fullName;
        final String phone;
        final boolean active;

        UserRow(long id, String email, String password, String fullName, String phone, boolean active) {
            this.id = id;
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.phone = phone;
            this.active = active;
        }
    }

    record HotelRow(long id, String name, String city, String address, double stars) {
    }

    record RoomRow(long id, long hotelId, String type, int capacity, BigDecimal basePrice, boolean active) {
    }

    record NightRow(long roomId, LocalDate date, String state, Long bookingId) {
    }

    static final class BookingRow {
        final long id;
        final long userId;
        final long roomId;
        final long hotelId;
        final LocalDate checkIn;
        final LocalDate checkOut;
        final int guests;
        String status;
        final BigDecimal total;
        String holdUntil;
        final Api.SnapshotView snapshot;

        BookingRow(long id, long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut,
                   int guests, String status, BigDecimal total, String holdUntil, Api.SnapshotView snapshot) {
            this.id = id;
            this.userId = userId;
            this.roomId = roomId;
            this.hotelId = hotelId;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.guests = guests;
            this.status = status;
            this.total = total;
            this.holdUntil = holdUntil;
            this.snapshot = snapshot;
        }
    }

    record PaymentRow(long id, long bookingId, long userId, BigDecimal amount, String method, String status) {
    }

    static final class DialogRow {
        final long id;
        final long userId;
        String status;

        DialogRow(long id, long userId, String status) {
            this.id = id;
            this.userId = userId;
            this.status = status;
        }
    }

    record MessageRow(long id, long dialogId, String author, String body) {
    }

    record NoteRow(long id, long userId, String kind, String body, String status, String createdAt) {
    }

    record SaleRow(long id, long hotelId, String status, BigDecimal amount, LocalDate day) {
    }

    record OccRow(long id, long hotelId, LocalDate day, int sold, int total) {
    }
}
