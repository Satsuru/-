package by.bsuir.platform.contracts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Клиентские контракты шлюза. Сейчас их реализуют заглушки.
 * Классы сервисов с базой эти интерфейсы не реализуют.
 */
public final class Clients {

    private Clients() {
    }

    public interface IdentityClient {
        Api.UserView me(long userId);

        Api.AuthPayload register(String email, String password, String fullName, String phone);

        Api.AuthPayload login(String email, String password);

        Long userIdByToken(String token);
    }

    public interface CatalogClient {
        Optional<Api.HotelView> hotel(long hotelId);

        List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars);

        Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut);

        boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut);

        void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId);

        void release(long bookingId);

        void confirmSold(long bookingId);
    }

    public interface PricingClient {
        Api.PriceQuote calculate(
                long hotelId,
                BigDecimal basePrice,
                BigDecimal occupancyRatio,
                LocalDate checkIn,
                LocalDate checkOut,
                int guests
        );
    }

    public interface BookingClient {
        List<Api.BookingView> listMine(long userId, String status, Api.BookingSort sort);

        Optional<Api.BookingView> get(long bookingId, long userId);

        Api.BookingStats stats(long userId);

        Api.BookingView create(long userId, long roomId, long hotelId, LocalDate checkIn, LocalDate checkOut, int guests, Api.PriceQuote quote);

        Api.BookingView pay(long userId, long bookingId);

        Api.BookingView cancel(long userId, long bookingId);
    }

    public interface PaymentClient {
        void complete(long bookingId, long userId, BigDecimal amount, String method);
    }

    public interface NotificationClient {
        List<Api.NotificationView> listMine(long userId);

        void add(long userId, String kind, String body);
    }

    public interface AssistantClient {
        Api.DialogView ask(long userId, Long dialogId, String guestText, String answer, boolean escalated);
    }

    public interface AnalyticsClient {
        Api.OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to);

        void addSale(long hotelId, String status, BigDecimal amount, LocalDate day);
    }
}
