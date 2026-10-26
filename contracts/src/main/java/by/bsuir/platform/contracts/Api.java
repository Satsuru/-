package by.bsuir.platform.contracts;

import java.math.BigDecimal;
import java.util.List;

public final class Api {

    private Api() {
    }

    public enum OfferSort { PRICE_ASC, PRICE_DESC, RATING_DESC }

    public enum BookingSort { DATE_ASC, DATE_DESC }

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

    public record AuthPayload(String token, UserView user) {
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

    public record BookingStats(Integer total, Integer created, Integer paid, Integer cancelled, Double paidSum) {
    }

    public record NotificationView(Long id, String kind, String body, String status, String createdAt) {
    }

    public record MessageView(Long id, String author, String body) {
    }

    public record DialogView(Long id, String status, List<MessageView> messages) {
    }

    public record OccupancyRevenueView(
            Long hotelId,
            Double occupancy,
            Double revenue,
            String periodFrom,
            String periodTo
    ) {
    }

    public record SearchInput(
            String city,
            String checkIn,
            String checkOut,
            Integer guests,
            Double minStars,
            OfferSort sort
    ) {
    }

    public record QuoteInput(Long roomId, String checkIn, String checkOut, Integer guests) {
    }

    public record PeriodInput(Long hotelId, String from, String to) {
    }

    public record BookingFilter(String status, BookingSort sort) {
    }

    public record RegisterInput(String email, String password, String fullName, String phone) {
    }

    public record LoginInput(String email, String password) {
    }

    public record CreateBookingInput(Long roomId, String checkIn, String checkOut, Integer guests) {
    }

    public record PayInput(Long bookingId, String method) {
    }

    public record CancelInput(Long bookingId) {
    }

    public record AskInput(String text, Long dialogId) {
    }
}
