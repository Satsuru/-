package by.bsuir.platform.gateway.mock;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import by.bsuir.platform.contracts.Failures;
import by.bsuir.platform.contracts.PriceMath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Заглушки клиентов шлюза. Модули сервисов сюда не подключены:
 * у каждого своё приложение и своя база.
 */
@Configuration
public class MockClients {

    @Bean
    MockWorld mockWorld() {
        return new MockWorld();
    }

    @Bean
    Clients.IdentityClient identityClient(MockWorld world) {
        return new Clients.IdentityClient() {
            @Override
            public Api.UserView me(long userId) {
                return world.user(userId).map(world::userView)
                        .orElseThrow(() -> new Failures.NotFoundException("Пользователь не найден"));
            }

            @Override
            public Api.AuthPayload register(String email, String password, String fullName, String phone) {
                if (email == null || !email.contains("@")) {
                    throw new Failures.BadRequestException("Укажите почту");
                }
                if (password == null || password.length() < 4) {
                    throw new Failures.BadRequestException("Пароль короче 4 символов");
                }
                if (fullName == null || fullName.isBlank()) {
                    throw new Failures.BadRequestException("Укажите имя");
                }
                boolean taken = world.users.stream().anyMatch(user -> user.email.equalsIgnoreCase(email));
                if (taken) {
                    throw new Failures.BadRequestException("Такая почта уже зарегистрирована");
                }
                long id = world.nextId();
                MockWorld.UserRow user = new MockWorld.UserRow(id, email.trim(), password, fullName.trim(), phone, true);
                world.users.add(user);
                world.roles.add(id + ":GUEST");
                return world.issue(user);
            }

            @Override
            public Api.AuthPayload login(String email, String password) {
                MockWorld.UserRow user = world.users.stream()
                        .filter(row -> row.email.equalsIgnoreCase(email) && row.password.equals(password))
                        .findFirst()
                        .orElseThrow(() -> new Failures.UnauthorizedException("Неверная почта или пароль"));
                return world.issue(user);
            }

            @Override
            public Long userIdByToken(String token) {
                return world.tokens.get(token);
            }
        };
    }

    @Bean
    Clients.CatalogClient catalogClient(MockWorld world) {
        return new Clients.CatalogClient() {
            @Override
            public Optional<Api.HotelView> hotel(long hotelId) {
                return world.hotel(hotelId).map(hotel -> new Api.HotelView(hotel.id(), hotel.name(), hotel.city(), hotel.address(), hotel.stars()));
            }

            @Override
            public List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
                return world.search(city, checkIn, checkOut, guests, minStars);
            }

            @Override
            public Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return world.room(roomId).map(room -> world.card(room, checkIn, checkOut));
            }

            @Override
            public boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return world.free(roomId, checkIn, checkOut);
            }

            @Override
            public void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
                world.hold(roomId, checkIn, checkOut, bookingId);
            }

            @Override
            public void release(long bookingId) {
                world.release(bookingId);
            }

            @Override
            public void confirmSold(long bookingId) {
                world.confirm(bookingId);
            }
        };
    }

    @Bean
    Clients.PricingClient pricingClient(MockWorld world) {
        return (hotelId, basePrice, occupancyRatio, checkIn, checkOut, guests) ->
                PriceMath.quote(basePrice, occupancyRatio, checkIn, checkOut, guests, world.rules);
    }

    @Bean
    Clients.BookingClient bookingClient(MockWorld world) {
        return new Clients.BookingClient() {
            @Override
            public List<Api.BookingView> listMine(long userId, String status, Api.BookingSort sort) {
                Comparator<MockWorld.BookingRow> order = Comparator.comparing(row -> row.checkIn);
                if (sort == Api.BookingSort.DATE_DESC) {
                    order = order.reversed();
                }
                return world.bookings.stream()
                        .filter(row -> row.userId == userId)
                        .filter(row -> status == null || status.isBlank() || status.equals(row.status))
                        .sorted(order)
                        .map(world::view)
                        .toList();
            }

            @Override
            public Optional<Api.BookingView> get(long bookingId, long userId) {
                return world.booking(bookingId, userId).map(world::view);
            }

            @Override
            public Api.BookingStats stats(long userId) {
                return world.stats(userId);
            }

            @Override
            public Api.BookingView create(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, Api.PriceQuote quote) {
                return world.createBooking(userId, roomId, hotelId, checkIn, checkOut, guests, quote);
            }

            @Override
            public Api.BookingView pay(long userId, long bookingId) {
                return world.pay(userId, bookingId);
            }

            @Override
            public Api.BookingView cancel(long userId, long bookingId) {
                return world.cancel(userId, bookingId);
            }
        };
    }

    @Bean
    Clients.PaymentClient paymentClient(MockWorld world) {
        return (bookingId, userId, amount, method) -> world.payments.add(
                new MockWorld.PaymentRow(world.nextId(), bookingId, userId, amount, method, "COMPLETED"));
    }

    @Bean
    Clients.NotificationClient notificationClient(MockWorld world) {
        return new Clients.NotificationClient() {
            @Override
            public List<Api.NotificationView> listMine(long userId) {
                return world.notes.stream()
                        .filter(note -> note.userId() == userId)
                        .map(note -> new Api.NotificationView(note.id(), note.kind(), note.body(), note.status(), note.createdAt()))
                        .toList();
            }

            @Override
            public void add(long userId, String kind, String body) {
                world.addNote(userId, kind, body);
            }
        };
    }

    @Bean
    Clients.AssistantClient assistantClient(MockWorld world) {
        return (userId, dialogId, guestText, answer, escalated) -> {
            MockWorld.DialogRow dialog;
            if (dialogId == null) {
                dialog = new MockWorld.DialogRow(world.nextId(), userId, escalated ? "ESCALATED" : "OPEN");
                world.dialogs.add(dialog);
            } else {
                dialog = world.dialogs.stream()
                        .filter(row -> row.id == dialogId && row.userId == userId)
                        .findFirst()
                        .orElseThrow(() -> new Failures.NotFoundException("Диалог не найден"));
                dialog.status = escalated ? "ESCALATED" : dialog.status;
            }
            world.messages.add(new MockWorld.MessageRow(world.nextId(), dialog.id, "GUEST", guestText));
            world.messages.add(new MockWorld.MessageRow(world.nextId(), dialog.id, "ASSISTANT", answer));
            List<Api.MessageView> thread = world.messages.stream()
                    .filter(message -> message.dialogId() == dialog.id)
                    .map(message -> new Api.MessageView(message.id(), message.author(), message.body()))
                    .toList();
            return new Api.DialogView(dialog.id, dialog.status, thread);
        };
    }

    @Bean
    Clients.AnalyticsClient analyticsClient(MockWorld world) {
        return new Clients.AnalyticsClient() {
            @Override
            public Api.OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to) {
                BigDecimal revenue = world.sales.stream()
                        .filter(sale -> sale.hotelId() == hotelId && "PAID".equals(sale.status()))
                        .filter(sale -> !sale.day().isBefore(from) && !sale.day().isAfter(to))
                        .map(MockWorld.SaleRow::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int sold = world.occupancy.stream()
                        .filter(row -> row.hotelId() == hotelId && !row.day().isBefore(from) && !row.day().isAfter(to))
                        .mapToInt(MockWorld.OccRow::sold).sum();
                int total = world.occupancy.stream()
                        .filter(row -> row.hotelId() == hotelId && !row.day().isBefore(from) && !row.day().isAfter(to))
                        .mapToInt(MockWorld.OccRow::total).sum();
                double ratio = total == 0 ? 0 : BigDecimal.valueOf(sold)
                        .divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP).doubleValue();
                return new Api.OccupancyRevenueView(hotelId, ratio, revenue.doubleValue(), from.toString(), to.toString());
            }

            @Override
            public void addSale(long hotelId, String status, BigDecimal amount, LocalDate day) {
                world.addSale(hotelId, status, amount, day);
            }
        };
    }

}
