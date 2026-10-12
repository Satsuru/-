package by.bsuir.platform.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Контракты между сервисами. По форме это будущие gRPC-методы.
 * В этой лабораторной вызов идёт в том же процессе, без сети.
 */
public final class Contracts {

    private Contracts() {
    }

    public record RoomCard(
            long roomId,
            long hotelId,
            String hotelName,
            String city,
            String address,
            double starRating,
            String roomType,
            int capacity,
            BigDecimal basePrice,
            BigDecimal occupancyRatio
    ) {
    }

    public record PriceQuote(
            BigDecimal nightly,
            BigDecimal total,
            BigDecimal occupancy,
            BigDecimal season,
            BigDecimal weekday,
            BigDecimal demand,
            BigDecimal guest,
            String explanation
    ) {
    }

    public record UserView(Long id, String email, String fullName, String phone, List<String> roles) {
    }

    public record HotelView(Long id, String name, String city, String address, Double starRating) {
    }

    public record RoomOfferView(
            Long roomId,
            HotelView hotel,
            String roomType,
            Integer capacity,
            Double basePrice,
            Double nightlyPrice,
            Boolean available
    ) {
    }

    public record MultipliersView(Double occupancy, Double season, Double weekday, Double demand, Double guest) {
    }

    public record QuoteView(
            Long roomId,
            String checkIn,
            String checkOut,
            Integer guests,
            Double basePrice,
            Double total,
            MultipliersView multipliers,
            String explanation
    ) {
    }

    public record SnapshotView(
            Double basePrice,
            Double occupancyMultiplier,
            Double seasonMultiplier,
            Double weekdayMultiplier,
            Double demandMultiplier,
            Double guestMultiplier,
            Double calculatedPrice
    ) {
    }

    public record BookingView(
            Long id,
            Long roomId,
            Long hotelId,
            String checkIn,
            String checkOut,
            Integer guests,
            String status,
            Double totalPrice,
            String holdUntil,
            SnapshotView snapshot
    ) {
    }

    public record NotificationView(Long id, String kind, String body, String status, String createdAt) {
    }

    public record OccupancyRevenueView(
            Long hotelId,
            Double occupancy,
            Double revenue,
            String periodFrom,
            String periodTo
    ) {
    }

    public record SearchInput(String city, String checkIn, String checkOut, Integer guests) {
    }

    public record QuoteInput(Long roomId, String checkIn, String checkOut, Integer guests) {
    }

    public record PeriodInput(Long hotelId, String from, String to) {
    }

    public interface IdentityPort {
        UserView me(long userId);
    }

    public interface CatalogPort {
        Optional<HotelView> hotel(long hotelId);

        List<RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests);

        Optional<RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut);

        boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut);
    }

    public interface PricingPort {
        PriceQuote calculate(
                long hotelId,
                BigDecimal basePrice,
                BigDecimal occupancyRatio,
                LocalDate checkIn,
                LocalDate checkOut,
                int guests
        );
    }

    public interface BookingPort {
        List<BookingView> listMine(long userId);

        Optional<BookingView> get(long bookingId, long userId);
    }

    public interface NotificationPort {
        List<NotificationView> listMine(long userId);
    }

    public interface AnalyticsPort {
        OccupancyRevenueView occupancyAndRevenue(long hotelId, LocalDate from, LocalDate to);
    }
}
